import java.util.Iterator;
import java.util.TreeSet;


public class Utils {
    /**
     * Metoda projede TreeSet predany v parametrech metody a zjisti, zda-li predany String jiz nachazi
     * v TreeSetu
     * @param item - zkoumany String
     * @param results - TreeSet ve kterem se item hleda.
     * @return
     */
    public static Word getWord (String item, TreeSet<Word> results) {
        Iterator<Word> it = results.iterator();

        while (it.hasNext()) {
            Word listItem = it.next();

            if (listItem.name.equals(item))
                return listItem;
        }
        return null;
    }
}
