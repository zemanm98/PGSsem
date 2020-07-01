
/**
 * Hlavni trida projektu obsahujici main metodu.
 */
public class MisserablessCount {
    /**
     * Nazev zpracovavaneho souboru
     */
    private static String INPUT = "lesmiserables_-_input.txt";

    /**
     * Main metoda projektu, zaklada hlavni vlakno, vola kontrolni metodu vszupnich argumentu
     * @param args - Vstupni argumenty s pocty vlaken ktere se maji spustit.
     */
    public static void main(String[] args) {

        //Kontrola vstupnich argumentu
        Konstanty.createNumbersofThreads(args);
        long zac = System.currentTimeMillis();
        Master master = new Master(INPUT);
        Thread thread = new Thread(master);
        thread.start();
        try{
            thread.join();
        }
        catch(InterruptedException e){
            System.err.println("Chyba");
        }
        long kon = System.currentTimeMillis();
        long vys = (kon - zac)/1000;
        System.out.println(vys);
        System.out.println("hele nechapu co tu poradne delam");



    }
}
