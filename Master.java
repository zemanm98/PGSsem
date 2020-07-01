import java.io.*;
import java.util.Iterator;
import java.util.TreeSet;


/**
 * Trida rozdelujici cely soubor do Svazku.
 */
public class Master implements Runnable {
    /**
     * Pocet Boss vlaken
     */
    private final int pocetbossu = Konstanty.POCET_BOSSU;
    /**
     * RandomAccesFile pro cteni ze vstupniho souboru
     */
    private RandomAccessFile soub;
    /**
     * Identifikacni cislo pro urceni o ktery Svazek se jedna
     */
    private int ID = 0;
    /**
     * Cesta k projektu od ktere se pozdeji odvozuje adresarovi system projektu
     */
    public static String path = System.getProperty("user.dir")+"\\lesmiserables";
    private boolean prvnivol = false;
    /**
     * TreeSet vysledku celeho romanu.
     */
    private TreeSet<Word> celyroman;
    /**
     * BufferedWriter pro zapis do souboru lesmiserables.txt
     */
    private BufferedWriter wrt;
    /**
     * BufferedWriter pro zapis do state.txt souboru.
     */
    private BufferedWriter wrtState;
    /**
     * Filelock pro zapis boss vlaken do state.txt
     */
    private final Object fileLockM = new Object();
    private final Object ulozLock = new Object();
    private final Object getLock = new Object();
    public static int b = 0;
     /**
     * Konstruktor tridy. Pripravuje soubor pro cteni a TreeSet vysledku.
     * @param name
     */
    public Master(String name){
        try{
            soub = new RandomAccessFile(name,"r");
        }
        catch (IOException e){
            e.printStackTrace();
        }
        celyroman = new TreeSet<Word>(new WordComparer());

    }

    /**
     * Metoda vytvari adresar do ktereho se soubory budou zapisovat a spousti podrazena boss vlakna.
     */
    @Override
    public void run() {
        System.out.println("jej");
        //Vytvareni lesmiserables adresare
        new File(path).mkdir();
        Thread[] bossove = new Thread[pocetbossu];

        //Inicializace state souboru
        try {
            File filS = new File(Master.path+"\\state.txt");
            FileWriter filvS = new FileWriter(filS);
            wrtState = new BufferedWriter(filvS);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        //Vytvareni Boss vlaken a jejich spousteni
        for(int i = 0 ; i < pocetbossu ; i++){
            bossove[i] = new Thread(new Boss(this, "Boss"+i));
            bossove[i].start();
        }

        for(Thread worker: bossove){
            try{
                worker.join();
            }
            catch (InterruptedException e){
                System.err.println("Boss "+worker+" neocekavane skoncil");
            }
        }

        try{
            printVysledek("lesmiserables.txt");
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * Metoda Cte ze vstupniho souboru a pomoc iregularniho vyrazu vraci vzdy jeden Volume do Boss vlaken.
     *
     * @return - Pokud je stale co cist vraci metoda Text jednoho svazku. Pokud jiz vlakno doslo na konec souboru
     *           vrati se $$konec$$
     */
    String getTextM(){
        synchronized (getLock){

            String line;
            String chapter = "";
            try {
                while ((line = soub.readLine()) != null) {
                    if(line.trim().matches("(VOLUME)\\b.*")){
                        prvnivol = true;
                        if(!chapter.equals("")) {
                            ID++;
                            return ID+" "+chapter.trim();
                        }
                    }
                    else{
                        if(prvnivol) {
                            chapter += line + "\n";
                        }
                    }

                }
                if(!chapter.equals("")){
                    ID++;
                    return ID+" "+chapter.trim();
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return "$$konec$$";
        }
    }

    /**
     * Ukladani vysledku celeho romanu do TreeSetu celyroman.
     * @param res - Treeset vysledku podrazeneho vlakna
     */
    void ulozVysledky(TreeSet<Word> res){
        synchronized (ulozLock){
            for (Word listItem : res) {
                Word w = Utils.getWord(listItem.name, celyroman);

                if (w == null)
                    celyroman.add(listItem);
                else {
                    celyroman.remove(w);
                    w.quantity += listItem.quantity;
                    celyroman.add(w);
                }
            }
        }
    }

    /**
     * Metoda zapise ziskane vysledky do patricneho souboru.
     * @param name - nazev souboru ktery se ma zapsat.
     * @throws IOException
     */
    private void printVysledek(String name) throws IOException {
        Iterator<Word> it = celyroman.iterator();
        try{
            File fi = new File(Master.path+"\\"+name);
            FileWriter filv = new FileWriter(fi);
            wrt = new BufferedWriter(filv);

        }
        catch (IOException e){
            e.printStackTrace();
        }

        while(it.hasNext()){
            Word listItem = it.next();
            wrt.newLine();
            wrt.write(listItem.name + " - " + listItem.quantity);
        }
        wrt.flush();
        wrt.close();
    }

    /**
     * Metoda zapisuje vsechna oznameni o ukonceni vlaken do souhrnneho state.txt souboru
     * @param name - text ktery se ma do souboru zapsat.
     */
    void printStateResultsM(String name){
        synchronized (fileLockM){
            try{
                wrtState.newLine();
                wrtState.write(name);
                wrtState.flush();
            }
            catch (IOException e){
                e.printStackTrace();
            }

        }

    }


}
