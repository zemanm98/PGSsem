import java.util.TreeSet;

/**
 * Trida ktera pocita slova v jednotlivych odstavcich
 */
public class Otrok implements Runnable{
    /**
     * Nazev vlakna
     */
    private String name;
    /**
     * Nadrazene vlakno
     */
    private Pracovnik prac;
    /**
     * Treeset vysledku z odstavce
      */
    private TreeSet<Word> result;

    /**
     * Konstruktor Tridy, Inicializuje Nadrazene vlakno, nazev.
     * @param pr - Nadrazene vlakno
     * @param nam - nazev vlakna
     */
    Otrok(Pracovnik pr, String nam){
        this.name = nam;
        this.prac = pr;

    }

    /**
     * Metoda pouze vola metodu spocitej.
     */
    @Override
    public void run() {
        spocitej();
    }


    /**
     * Metoda pocita slova v jednotlivych odstavcich ziskavanych metodou getTextC() nadrazeneho vlakna
     */
    private void spocitej() {
        int i;
        String chapter;

        //While cyklus ktery si zada text odstavce z Metody nadrazeneho vlakna, dokud je nejaky text k dispozici.
        while (!(chapter = prac.getTextC()).equals("$$konec$$")) {
            if(chapter.equals("$$last$$")){
                this.prac.waitEnding();
            }
            if(chapter.equals("$$newjob$$")){
                this.prac.waitEnding();
            }
            else{
                //Inicializace Treesetu vysledku
                result = new TreeSet<Word>(new WordComparer());

                //Pole Stringu jednotlivych slov rozdelenych regularnim vyrazem.
                String[] words = chapter.split("[^A-Za-z0-9]+");

                //For cyklus projizdi pole slov z odstavce
                for (i = 0; i < words.length; i++) {
                    if(words[i].equals("")){
                        continue;
                    }
                    //Prevedeni slov na mala pismena
                    words[i] = words[i].toLowerCase();

                    //Kontrola vyskytu slova v TreeSetu vysledku.
                    Word w = Utils.getWord(words[i], result);

                    //Pokud se slovo ve vysledku nevyskytuje, tak se do nej prida s cetnosti 1.
                    if (w == null) {
                        w = new Word();
                        w.name = words[i];
                        w.quantity = 1;
                    }
                    //Pokud slovo ve vysledku jiz existovalo, pouze se prida cetnost
                    else {
                        result.remove(w);
                        w.quantity++;
                    }

                    result.add(w);

                }
                //Po dokonceni fro cyklu se vysledek preda nadrazenemu vlaknu
                prac.ulozVysledky(result);
            }
        }
        this.prac.waitEnding();



    }
}
