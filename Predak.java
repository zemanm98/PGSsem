import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Trida Predak predstavuje vlakna ktera se staraji o prerozdeleni jedne knihy na kapitoly. Rozdelene kapitoly pote predava dal Pracovnikum.
 */
public class Predak implements  Runnable{
    /**
     * Reference na nadrazene vlakno
     */
    private Boss boss;
    /**
     * nazev vlakna
     */
    private String name;
    /**
     * pocet pracovniku ktere se pod timto vlaknem spusti
     */
    private final int pocetPrac = Konstanty.POCET_PRACOVNIKU;
    /**
     * Identifikacni cislo urcujici o kolikatou kapitolu v teto knize se jedna
     */
    private int ID = 0;
    /**
     * TreeSet vyslednych slov a jejich cetnosti
     */
    private TreeSet<Word> resultBook;
    /**
     * BufferedWriter ktery zapisuje do souboru Bookx.txt
     */
    private BufferedWriter write;
    /**
     * BufferedWriter zapisujici dokoncena vlakna do souboru state.txt
     */
    private BufferedWriter writeState;
    /**
     * Pole stringu ktere si uchovava jednotlive kapitoly po splitu cele knihy
     */
    private String [] book;
    /**
     * Pole stringu ktere urcuje ve kterem je vlakno Volumu, a Booku
     */
    private String [] oznaceni;
    /**
     * Object pro synchronizaci vlaken pro zapisovani do souboru state.txt
     */
    private final Object fileLock = new Object();
    /**
     * Zamek pro metodu ulozVysledky().
     */
    private final Object ulozLock = new Object();
    /**
     * Zamek pro metodu getTextP().
     */
    private final Object getLock = new Object();
    /**
     * Integer urcuje v jakem stavu se nachazi prace urcena tomuto vlaknu. Pokud je 0, vlakno ma praci. Pokud -1, tak na praci ceka, pokud -2, zadna prace jiz neni.
     */
    private int job = 0;
    /**
     * Cyklicka bariery tridy.
     */
    private CyclicBarrier cb;
    /**
     * Konstruktor Tridy ve kterem se uchovava reference na nadrazene vlakno a inicializuje nazev vlakna.
     * @param bos - nadrazene vlakno
     * @param nam - nazev vlakna
     */
    public Predak (Boss bos, String nam){
        this.boss = bos;
        this.name = nam;
        //Cyklicka bariera pro zapis vysledku a pozadani o dalsi text.
        cb =  new CyclicBarrier(pocetPrac, new Runnable() {
            @Override
            public void run() {
                if (job == 0) {
                    //Ukladani vysledku do Treesetu nadrazeneho vlakna.
                    boss.ulozVysledky(resultBook);

                    //Vypsani vysledku do souboru Bookx.txt
                    try {
                        printResultB("Book" + oznaceni[1].trim());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //Predani inforamce o dokonceni vlakna vyssimu vlaknu
                    boss.printStateResults("Book" + oznaceni[1].trim() + " - OK");

                }
                job = loadBook();
            }
        });

    }

    /**
     * Metoda spusti podrazena vlakna Pracovniku a ceka na jejich ukonceni.
     */
    @Override
    public void run() {
        job = loadBook();


        //Pole podrazenych vlaken
        Thread[] pracovnici = new Thread[pocetPrac];

        //vytvareni a spousteni podrazenych vlaken pracovnici
        for (int i = 0; i < pocetPrac; i++) {
            pracovnici[i] = new Thread(new Pracovnik(this, "Pracovnik" + i));
            pracovnici[i].start();
        }

        for (Thread worker : pracovnici) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                System.err.println("Boss " + worker + " neocekavane skoncil");
            }
        }
        this.boss.waitEnding();



    }





    /**
     * Metoda vraci jednu kapitolu z knihy. kapitoly jsou rozdeleny v poli Stringu book.
     * Vraci String v zavislosti na stavu prace. Pokud v poli book jsou stale knihy na rozdeleni, varti jednu z nich, jinak vraci jeden
     * ze stavovych Stringu, ktere rozhoduji u nizsich vlaknech o dalsim prubehu.
     * @return - Text Chapteru, nebo jeden ze stavovach Stringu
     */
    public String  getTextP(){
        synchronized (getLock){
            if(job == 0) {
                if(ID < book.length - 1) {
                    ID++;
                    return oznaceni[0].trim() + "-" + oznaceni[1].trim() + "-" + ID + "" + book[ID];

                }
                else {
                    return "$$last$$";
                }
            }
            else if(job == -1){
                return "$$newjob$$";
            }
            else{
                return "$$konec$$";
            }
        }
    }

    /**
     * Metoda nacita z rodicovskeho vlakna novy text a rozhoduje o stavu prace na zaklade predaneho textu.
     *
     * @return
     */
    private synchronized int loadBook(){
        String loadbok = boss.getTextB();
        if(loadbok.equals("$$konec$$")){
            return -2;
        }
        if(loadbok.equals("$$last$$")){
            boss.waitEnding();
            return -1;
        }
        else{
            resultBook = new TreeSet<Word>(new WordComparer());
            //Pokud vlakno dostane dalsi knihu, musi nejprve vynulovat Identifikacni cislo a knihu splitnout na kapitoly.
            ID = 0;
            book = loadbok.split("(CHAPTER)\\b.*");

            //Na nultem indexu po splitu zustavaji cisla urcujici ve kterem je vlakno Volumu a Booku.
            oznaceni = book[0].split("[^0-9]+");

            //Zjisteni zda-li existuje slozka do ktere bude vlakno soubory ukladat, popripade ho zalozi.
            if(!Files.exists(Paths.get(Master.path + "\\" + "Volume" + oznaceni[0].charAt(0)+"\\"+"Book"+oznaceni[1].trim()))){
                new File(Master.path+"\\"+"Volume" + oznaceni[0].charAt(0)+"\\"+"Book"+oznaceni[1].trim()).mkdir();
            }

            //Zalozeni souboru state.txt ve spravnem adresari.
            try{
                File filestate = new File(Master.path+"\\"+"Volume"+oznaceni[0].charAt(0)+"\\"+"Book"+oznaceni[1].trim()+"\\state.txt");
                FileWriter filvs = new FileWriter(filestate);
                writeState = new BufferedWriter(filvs);
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return 0;
        }
    }

    /**
     * Metoda uklada do Treesetu resultBook, ktery predstavuje vysledky slov v cele
     * knize vlakna.
     * @param res - predany Treeset z nizsiho vlakna Pracovniku.
     */
    public void ulozVysledky(TreeSet<Word> res) {
        synchronized (ulozLock) {

                Iterator<Word> it = res.iterator();
                while (it.hasNext()) {
                    Word listItem = it.next();

                    Word w = Utils.getWord(listItem.name, resultBook);

                    if (w == null)
                        resultBook.add(listItem);
                    else {
                        resultBook.remove(w);
                        w.quantity += listItem.quantity;
                        resultBook.add(w);
                    }
                }

        }
    }

    /**
     * Metoda zapisuje vysledek ziskany vlaknem do souboru Bookx.txt
     * @param name - Nazev souburo ktery se ma zapsat.
     * @throws IOException
     */
    public synchronized void printResultB(String name) throws IOException {
        Iterator<Word> it = resultBook.iterator();

        //Vytvoreni prislusneho souboru
        try{
            File fi = new File(Master.path+"\\"+"Volume"+oznaceni[0].charAt(0)+"\\"+name+"\\"+name+".txt");
            FileWriter filv = new FileWriter(fi);
            write = new BufferedWriter(filv);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        //samotny zapis
        while(it.hasNext()){
            Word listItem = it.next();
            write.newLine();
            write.write(listItem.name + " - " + listItem.quantity);
        }
        write.flush();
        write.close();
    }

    /**
     * Metoda zapisuje do souboru state.txt a zajistuje zapisovani do vyssiho vlakna.
     * @param name - Text ktery se ma to state.txt zapsat.
     */
    public void printStateResult(String name){
        synchronized (fileLock){
            try{
                writeState.newLine();
                writeState.write(name);
                writeState.flush();
                boss.printStateResults("Book"+this.oznaceni[1].trim()+" - "+name);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    /**
     * Metoda vola nad cyklickou barierou cb await().
     */
    public void waitEnding(){
        try {
            cb.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
