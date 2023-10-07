import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * A query engine for the Jeopardy questions provided.
 *
 * Builds queries from the Jeopardy questions and category hints and performs searches on the
 * index. After retrieving answers, prints and calculate statistics about the efficiency of the system.
 */
public class QueryEngine {
    File queries;
    Directory index;
    IndexSearcher searcher;
    static EnglishAnalyzer analyzer = new EnglishAnalyzer();
    int SEARCH_HITS_PER_PAGE = 10;

    /**
     * Instantiates index and IndexSearcher.
     *
     * @param queriesFilePath       path of the .txt file containing the queries
     * @param indexDirectoryPath    path of the directory containing the Index
     * @throws IOException If a required file or directory cannot be opened
     */
    public QueryEngine(String queriesFilePath, String indexDirectoryPath) throws IOException {
        index = FSDirectory.open(new File(indexDirectoryPath).toPath());
        queries = new File(queriesFilePath);
        searcher = new IndexSearcher(DirectoryReader.open(index));
        searcher.setSimilarity(new BM25Similarity((float) 1.14, (float) 0.15));
    }

    /**
     * Builds queries and performs search on the Index.
     *
     * This method builds boolean queries and performs searches while recording the number of
     * accurate search hits at each position.
     *
     * @throws ParseException   if a BooleanQuery passed to the IndexSearcher is not formatted correctly
     * @throws IOException      if an important file or directory cannot be accessed.
     */
    public void performQueries() throws ParseException, IOException {
        System.out.print("---------------QUERY RESULTS--------------------------");
        Scanner sc = new Scanner(queries);

        HashMap<Integer, Integer> stat_hits_at_position = new HashMap<>();

        int questionNumber = 1;
        while(sc.hasNextLine()) {
            // Parse the question, hints, and expected answer
            String category = sc.nextLine().replaceAll("\\p{Cntrl}", "");
            String originalClue = sc.nextLine();
            String formattedClue = originalClue.replaceAll("\\p{Cntrl}", "");
            String answer = sc.nextLine();
            sc.nextLine();  // Move to next query

            // Build queries
            BooleanQuery query = buildQuery(formattedClue, category);

            // Perform search and get ranked results
            ScoreDoc[] searchResults = searcher.search(query, SEARCH_HITS_PER_PAGE).scoreDocs;
            int hit_position = analyzeResults(questionNumber, answer, searchResults);

            // Update the number of hits for the position at which the right answer was found
            int current_hits = stat_hits_at_position.getOrDefault(hit_position, 0);
            stat_hits_at_position.put(hit_position, current_hits + 1);

            // Update question number
            questionNumber++;
        }
        sc.close();
        index.close();
        // Calculate and print statistics about the efficiency of the system
        printStatistics(stat_hits_at_position);
    }

    /**
     * Builds a multi-field BooleanQuery using the clue and the category hints.
     *
     * This method creates a BooleanQuery using three types of queries:
     *  - A query for the `category` field of the indexed documents using the category hints.
     *  - A query for the `body` field of the indexed documents using the clue and the category hints.
     *  - Positional queries for quotes in the clue if required.
      * @param clue     the Jeopardy question
     * @param category  categories in Jeopardy
     * @return          a boolean query that will be performed on the indexes
     * @throws ParseException   if the query is not formatted correctly using Lucene syntax
     */
    private BooleanQuery buildQuery(String clue, String category) throws ParseException {
        QueryParser bodyQueryParser = new QueryParser("body", analyzer);

        ArrayList<Query> queries = new ArrayList<>();
        // If the Jeopardy question has quotes, add a phrase search query for each quote
        if(hasQuotes(clue)) {
            queries.addAll(buildPhraseQueries(clue, bodyQueryParser));
        }

        // Create a query for the document body using the Jeopardy question and the clue hints
        Query standardQuery = bodyQueryParser.parse(QueryParser.escape(clue + "\n" + category));
        queries.add(standardQuery);

        // Create a query just for the categories
        QueryParser categoryQueryParser = new QueryParser("categories", analyzer);
        Query categoryQuery = categoryQueryParser.parse(QueryParser.escape(category));
        queries.add(categoryQuery);

        // Create a boolean query using all the previously created queries
        BooleanQuery.Builder booleanBuilder = new BooleanQuery.Builder();
        for (Query query: queries) {
            booleanBuilder.add(query, BooleanClause.Occur.SHOULD);
        }
        return booleanBuilder.build();
    }

    /**
     * Checks whether a Jeopardy question has quotes in it.
     * @param clue  the Jeopardy question
     * @return true if the question has quotes, false otherwise
     */
    private boolean hasQuotes(String clue) {
        return clue.contains("\"");
    }

    /**
     * Builds a list of phrase queries for quotes in the Jeopardy question.
     *
     * This method separates out each quote in the Jeopardy question and creates a query for it
     * to increase the score if the quote exists in an indexed document.
     * @param clue          the Jeopardy question
     * @param queryParser   the Lucene query parser.
     * @return              a list of all phrase queries
     * @throws ParseException if a query is not formatted correctly using Lucene syntax.
     */
    private ArrayList<Query> buildPhraseQueries(String clue, QueryParser queryParser) throws ParseException {
        // Split the Jeopardy question to locate quotes
        String[] splitClue = clue.split("\"");
        ArrayList<Query> phraseQueries = new ArrayList<>();
        for(int i = 1; i < splitClue.length; i += 2) {
            // every odd element is a quote
            String phraseQuery = QueryParser.escape(splitClue[i]);
            phraseQuery = "\"" + phraseQuery + "\"2.5"; // phrase query
            phraseQueries.add(queryParser.parse(phraseQuery));
        }
        return phraseQueries;
    }

    /**
     * Checks whether the right answers were retrieved for the queries and prints returns the position
     * of that document in the search result.
     *
     * @param questionNumber    the position of that question in the set of questions, for printing
     * @param answer            the expected answer to that question
     * @param searchResults     the search results for that question
     * @return                  the position at which the right answer was found
     * @throws IOException      if the indexed document cannot be retrieved from the index
     */
    private int analyzeResults(int questionNumber, String answer, ScoreDoc[] searchResults) throws IOException {
        int hit_position = 0;
        System.out.println("\nAnswer to Question " + questionNumber + ": " + answer);
        for(int i = 0; i < searchResults.length; i++) {
            ScoreDoc result = searchResults[i];
            String resultTitle = searcher.doc(result.doc).get("title");
            // Return the position of the expected answer
            if(resultTitle.toLowerCase().matches(answer.toLowerCase())) {
                System.out.println("Hit at position: " + (i+1));
                hit_position = i + 1;
                return hit_position;
            }
        }
        // If the expected answer was not retrieved
        return hit_position;
    }

    /**
     * Calculates and prints the number of questions for which the right answer was found in the
     * top 10 results and the Precision at 1 of the system.
     *
     * @param stat_hits_at_position dictionary mapping positions in search results to the number of questions for which the right answer was found at that position
     */
    private void printStatistics(HashMap<Integer, Integer> stat_hits_at_position) {
        System.out.println("---------------STATISTICS-----------------------------");
        // hits_at_one helps us calculate the Precision at 1
        int hits_at_one = stat_hits_at_position.getOrDefault(1, 0);
        int hits_in_top_ten = 0;
        for(int i = 1; i <= 10; i++) {
            int hits_at_i = stat_hits_at_position.getOrDefault(i, 0);
            System.out.println("Docs in position " + i + ": " + hits_at_i);
            hits_in_top_ten += hits_at_i;
        }

        // Print statistics
        System.out.println("\nHits in top 10 Documents: " + hits_in_top_ten);
        System.out.println("P@1: " + (hits_at_one/100.0));
    }
}