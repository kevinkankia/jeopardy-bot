import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

/**
 * The driver for the system. Creates the Indexes and performs the search.
 */
public class Main {
    private static final String WIKI_DIR_PATH = "src/main/resources/wiki-data";
    private static final String INDEX_DIR_PATH = "src/main/resources/indexed-docs";
    private static final String QUERIES_FILE_PATH = "src/main/resources/queries.txt";

    public static void main(String[] args) {
        try {
            Indexer indexer = new Indexer(WIKI_DIR_PATH, INDEX_DIR_PATH);
            indexer.buildIndex();
            QueryEngine queryEngine = new QueryEngine(QUERIES_FILE_PATH, INDEX_DIR_PATH);
            queryEngine.performQueries();
        } catch(IOException e) {
            throw new RuntimeException("This should never happen, all files exist.", e);
        } catch (ParseException e) {
            throw new RuntimeException("This should never happen, all queries are built by Lucene parsers.", e);
        }
    }
}
