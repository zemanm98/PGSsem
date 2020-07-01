import java.util.Comparator;

/**
 * Trida zajistuje komparator pro ukladani slov to Treesetu
 */
public class WordComparer implements Comparator<Word> {
    public int compare(Word arg0, Word arg1) {
        return arg0.name.compareTo(arg1.name);
    }
}
