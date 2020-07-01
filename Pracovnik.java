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
 * Trida Pracovnik prelozdeluje ziskanou kapitolu do odstavcu a vysledky uklada do nadrazeneho vlakna.
 */
public class Pracovnik implements Runnable{
    /**
     * Nazev vlakna
     */
    private String name;
    /**
     * Nadrazene vlakno
     */
    private Predak predak;
    /**
     * Pocet podrazenych vlaken Otroku ktere se maji spustit
     */
    private final int pocetOtr = Konstanty.POCET_OTROKU;
    /**
     * BufferedWriter ktery zapisuje do souboru Chapterx.txt
     */
    private BufferedWriter writ;
    /**
     * TreeSet vysledku
     */
    private TreeSet<Word> resultC = null;
    /**
     * Pole stringu obsahujicich jednotlive odstavce
     */
    private String [] chapter;
    /**
     * Pole stringu oznacujici ve kterem Volumu, Booku a Chapteru se vlakno nachazi.
     */
    private String[] oznacen;
    /**
     * Cislo urcujici ve while cyklu metody getTextC() ktery odstavec se preda.
     */
    private int ID = 0;
    /**
     * Zamek pro synchronizec metodu getTextC().
     */
    private final Object filelock = new Object();
    /**
     * Zamek pro metodu ulozVysledky().
     */
    private final Object ulozLock = new Object();
    /**
     * Zamek pro metodu printResultC().
     */
    private final Object printLock = new Object();
    /**
     * Integer urcuje v jakem stavu se nachazi prace urcena tomuto vlaknu. Pokud je 0, vlakno ma praci. Pokud -1, tak na praci ceka, pokud -2, zadna prace jiz neni.
     */
    private int job = 0;
    /**
     * Cyklicka bariera tridy
     */
    private CyclicBarrier cb;
    /**
     * Konstruktor Tridy. Inicializuje se zde jmeno, nadrazene vlakno a Cyklicke bariery, ktera zadrzi vlakna, ktera jiz dopocitala.
     * Ulozi a vypise vysledky a pozada o novy text.
     * @param pred - Nadrazene vlakno
     * @param nam - nazev vlakna
     */
    Pracovnik(Predak pred, String nam){
        this.name = nam;
        this.predak = pred;
        //Cyklicka bariera pro zapis vysledku a pozadani o dalsi text.
        cb =  new CyclicBarrier(pocetOtr, new Runnable() {
            @Override
            public void run() {
                if(job == 0) {
                    predak.ulozVysledky(resultC);
                    try {
                        printResultC("Chapter" + oznacen[2].trim());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //Predani informace o dokonceni vlakna do vyssiho vlakna
                    predak.printStateResult("Chapter" + oznacen[2].trim() + " - OK");

                }
                job = loadChapter();
             }
        });

    }

    /**
     * Metoda spusti podrazena vlakna Otroku a ceka na jejich ukonceni.
     */
    @Override
    public void run() {
            job = loadChapter();
            Thread[] otroci = new Thread[pocetOtr];
            for (int i = 0; i < pocetOtr; i++) {
                otroci[i] = new Thread(new Otrok(this, "Otrok" + i));
                otroci[i].start();
            }

            for (Thread worker : otroci) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    System.err.println("Boss " + worker + " neocekavane skoncil");
                }
            }
            this.predak.waitEnding();
    }

    /**
     * Metoda ktera vraci jeden odstavec chapteru vlakna, nebo jeden ze Stringu last, newjob a konec.
     * Podle navratove hodnoty pak Otrok vlakno bud skonci, nebo zavola metodu waitEnding().
     * @return - Text odstavce
     */
    String  getTextC(){
        synchronized (filelock) {
            if(job == 0) {
                if (ID < chapter.length -1) {
                    ID++;
                    return chapter[ID];
                } else {

                    return "$$last$$";
                }
            }
            else if(job == -1) {
                return "$$newjob$$";
            }
            else{
                return "$$konec$$";
            }

        }
    }

    /**
     * Metoda nacte novy chapter z vlakna rodic. V zavislosti na navratove hodnote, rozhodne o stavu promenne job.
     * @return stav prace pro vlakno.
     */
    private synchronized int loadChapter(){
        String nacteny_text = predak.getTextP();
        if(nacteny_text.equals("$$konec$$")) {
            predak.waitEnding();
            return -2;
        }
        if(nacteny_text.equals("$$last$$") || nacteny_text.equals("$$newjob$$")){
            predak.waitEnding();
            return -1;
        }

        else{
            resultC = new TreeSet<Word>(new WordComparer());
            ID = 0;
            chapter = nacteny_text.split("\n\n");
            oznacen = chapter[0].split("[^0-9]+");
            if (!Files.exists(Paths.get(Master.path + "\\" + "Volume" + oznacen[0].charAt(0) + "\\" + "Book" + oznacen[1].trim()))) {
                new File(Master.path + "\\" + "Volume" + oznacen[0].charAt(0) + "\\" + "Book" + oznacen[1].trim()).mkdir();
            }
            return 0;

        }
    }

    /**
     * Metoda uklada vysledky nizsich vlaken do TreeSetu vysledku tohoto vlakna
     * @param res - Treeset vysledku vlakna Otrok
     */
    void ulozVysledky(TreeSet<Word> res) {
        synchronized(ulozLock) {
            for (Word listItem : res) {
                Word w = Utils.getWord(listItem.name, resultC);

                if (w == null)
                    resultC.add(listItem);
                else {
                    resultC.remove(w);
                    w.quantity += listItem.quantity;
                    resultC.add(w);

                }
            }


        }
    }

    /**
     * Metoda zapise vysledky do prislusneho souboru jehoz nazev je predan v parametru metody
     * @param s - nazev souboru ktery se ma zapsat.
     * @throws IOException
     */
    private void printResultC(String s) throws IOException {
        synchronized (printLock) {

                Iterator<Word> it = resultC.iterator();
                try {
                    File fi = new File(Master.path + "\\" + "Volume" + oznacen[0].charAt(0) + "\\" + "Book" + oznacen[1].trim() + "\\" + s + ".txt");
                    FileWriter filv = new FileWriter(fi);
                    writ = new BufferedWriter(filv);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (it.hasNext()) {
                    Word listItem = it.next();
                    writ.newLine();
                    writ.write(listItem.name + " - " + listItem.quantity);
                }
                writ.flush();
                writ.close();
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
