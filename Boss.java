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
 * Trida predstavuje vlakna ktera text svazku rozdeli po knihach a predaji podrazenym vlaknum.
 */
public class Boss implements Runnable{
    /**
     * Pocet podrazenych vlaken ktere se spusti
     */
    private final int pocetPredaku = Konstanty.POCET_PREDAKU;
    /**
     * Nazev vlakna
     */
    private String name;
    /**
     * Reference na nadrazene vlakno
     */
    private Master master;
    /**
     * Identifikacni cislo urcujici kterou knihu predava dal
     */
    private int ID = 0;
    /**
     * TreeSet vysledku
     */
    private TreeSet<Word> resultVolume;
    /**
     * BufferedWriter pro zapis do souboru Volumex.txt
     */
    private BufferedWriter write;
    /**
     * BufferedWriter pro zapis do state souboru
     */
    private BufferedWriter writeState;
    /**
     * Pole do ktereho se rozdeli vsechny knihy svazku
     */
    private String [] volume;
    /**
     * Oznaceni svazku ve kterem se vlakno nachazi
     */
    private String oznaceni;
    /**
     * Object pro synchronizaci claken pro zapis do state souboru
     */
    private final Object FileLock = new Object();
    /**
     * Zamek pro metodu ulozVysledky().
     */
    private final Object ulozLock = new Object();
    /**
     * Zamek pro metodu getTextB().
     */
    private final Object getLock = new Object();
    /**
     * Cyklicka bariera tridy.
     */
    private CyclicBarrier cb;
    /**
     * Promenna job rozhoduje o stavu prace vlakna, ale narozdil od nizsich vlaknech rika pouze pokud prace jeste je, nebo jiz ne. (True / False).
     */
    private boolean job = false;
    /**
     * Konstruktor Tridy, inicializuje jmeno, nadrazene master vlakno.
     * @param mstr
     * @param name
     */
    Boss(Master mstr, String name){
        this.master = mstr;
        this.name = name;
        //Cyklicka bariera pro zapis vysledku a pozadani o dalsi text.
        cb =  new CyclicBarrier(pocetPredaku, new Runnable() {
            @Override
            public void run() {
                if(job) {
                    //Ukladani vysledku do nadrazeneho vlakna
                    master.ulozVysledky(resultVolume);
                    resultVolume.clear();
                    //Vypis vysledku do Volumex.txt souboru
                    try {
                        printResultV("Volume" + oznaceni);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //Ulozeni informace o dokonceni vlakna do master vlakna
                    master.printStateResultsM("Volume" + oznaceni + " - OK");

                }
                job = loadVolume();
            }

        });

    }

    /**
     * Metoda spousti podrazena vlakna Predaku a ceka na jejich ukonceni.
     */
    @Override
    public void run() {
        job = loadVolume();
        //Vytvareni a spousteni podrazenych vlaken
        Thread[] predaci = new Thread[pocetPredaku];
        for (int i = 0; i < pocetPredaku; i++) {
            predaci[i] = new Thread(new Predak(this, "Predak" + i));
            predaci[i].start();
        }

        for (Thread worker : predaci) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                System.err.println("Boss " + worker + " neocekavane skoncil");
            }
        }



    }




    /**
     * Metoda predava do podrazenych vlaken knihy svazku.
     * @return - Text knihy pokud nejsme v poli knih nakonci, jinak vraci $$last$$ a v pripade ze jiz neni zadna prace, tak vraci "$$konec$$".
     */
    String  getTextB(){
        synchronized (getLock){
            if(job) {
                if (ID < volume.length - 1) {
                    ID++;
                    return oznaceni + "-" + ID + "" + volume[ID];

                }
                else{
                    return "$$last$$";
                }
            }
            else {
                return "$$konec$$";
            }
        }
    }

    /**
     * Metoda nacita pomoci volani metody getTextM rodicovskeho vlakna jednotlive Volumes textu.
     * @return - vraci true/false na zaklade toho, jestli prace stale je, nebo jiz neni.
     */
    private synchronized boolean loadVolume(){
        String nactenyvolume = master.getTextM();
        if(nactenyvolume.equals("$$konec$$")){
            return false;
        }
        else{
            resultVolume = new TreeSet<Word>(new WordComparer());
            ID = 0;

            //Rozdeleni svazku na knihy
            volume = nactenyvolume.split("(BOOK)\\b.*");

            //Prvni char v rozdelenem textu oznacuje ve kterem svazku se vlakno nachazi
            oznaceni = String.valueOf(volume[0].charAt(0));

            //Zalozeni patricneho adresare
            if (!Files.exists(Paths.get(Master.path + "\\" + "Volume" + oznaceni))) {
                new File(Master.path + "\\" + "Volume" + oznaceni).mkdir();
            }

            //Zalozeni state souboru
            try{
                File files = new File(Master.path+"\\"+"Volume"+oznaceni+"\\state.txt");
                FileWriter filves = new FileWriter(files);
                writeState = new BufferedWriter(filves);
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return true;
        }
    }

    /**
     * Metoda ukalada vysledky podrazenych vlaken do vysledku resultVolume
     * @param res - TreeSet vysledku podrazeneho vlakna
     */
    void ulozVysledky(TreeSet<Word> res){
        synchronized (ulozLock) {

                for (Word listItem : res) {
                    Word w = Utils.getWord(listItem.name, resultVolume);

                    if (w == null)
                        resultVolume.add(listItem);
                    else {
                        resultVolume.remove(w);
                        w.quantity += listItem.quantity;
                        resultVolume.add(w);
                    }
                }
            }

    }

    /**
     * Zapis vysledku scitani do souboru Volumex.txt
     * @param name - nazev souboru ktery se ma vytvorit
     * @throws IOException
     */
    private synchronized void printResultV(String name) throws IOException {
        Iterator<Word> it = resultVolume.iterator();
        try{
            File fi = new File(Master.path+"\\"+name+"\\"+name+".txt");
            FileWriter filv = new FileWriter(fi);
            write = new BufferedWriter(filv);

        }
        catch (IOException e){
            e.printStackTrace();
        }

       // it.next();
        while(it.hasNext()){
            Word listItem = it.next();
            write.newLine();
            write.write(listItem.name + " - " + listItem.quantity);
        }
        write.flush();
        write.close();
    }

    /**
     * Metoda Zapisuje informace o ukonceni vlaken a predava informaci do vyssiho vlakna
     * @param name - Text ktery se ma zapsat
     */
    void printStateResults(String name){
        synchronized (FileLock){
            try{
                writeState.newLine();
                writeState.write(name);
                writeState.flush();
                master.printStateResultsM("Volume"+this.oznaceni+" - "+name);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    /**
     * Metoda vola nad barierou cb await().
     */
    public void waitEnding(){
        try {
            cb.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }


}
