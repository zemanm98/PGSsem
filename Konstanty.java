public class Konstanty {
    /**
     * Pocty vlaken ktere se vytvori
     */
    public static int POCET_BOSSU;
    public static int POCET_PREDAKU;
    public static int POCET_PRACOVNIKU;
    public static int POCET_OTROKU;

    /**
     * Metoda zkontroluje a priradi pocty vlaken konstantam.
     * @param args - Vstupni argumenty programu.
     */
    public static void createNumbersofThreads(String[] args){
        if(args.length != 4){
            System.out.println("Chybne zadany pocet vlaken");
            System.out.println("Boii, tak snad to už gunhuje");
            for(int i = 0 ; i < 6; i ++){
                System.out.println("hm,m");
            }
            System.out.println("Asi nevim jak to funguje");
            System.exit(0);
        }
        try{
            POCET_BOSSU = Integer.parseInt(args[0]);
            POCET_PREDAKU = Integer.parseInt(args[1]);
            POCET_PRACOVNIKU = Integer.parseInt(args[2]);
            POCET_OTROKU = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException e){
            System.out.println("Chybne zadane cislo");
            System.exit(0);
        }
    }
}
