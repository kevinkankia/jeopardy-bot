import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * An indexer that parses text files to create a Positional Index of Wikipedia pages.
 */
public class Indexer {
    private File wikiDirectory;
    private Directory index;

    IndexWriterConfig writerConfig;
    IndexWriter writer;

    /**
     * Instantiates the IndexWriter and the directories required.
     *
     * @param wikiDirectoryPath     path of the wiki documents that need to be indexed
     * @param indexDirectoryPath    path where the index built will be stored
     * @throws IOException          if a required file or directory cannot be accessed
     */
    public Indexer(String wikiDirectoryPath, String indexDirectoryPath) throws IOException {
        this.wikiDirectory = new File(wikiDirectoryPath);
        this.index = FSDirectory.open((new File(indexDirectoryPath).toPath()));

        writerConfig = new IndexWriterConfig(new EnglishAnalyzer());
        writer = new IndexWriter(this.index, this.writerConfig);
    }

    /**
     * Loops through each file containing wikipedia pages to build and documents for the index.
     *
     * @throws IOException  if a required file or directory cannot be accessed
     */
    public void buildIndex() throws IOException {
        System.out.println("---------------BUILDING INDEX-------------------------");

        File[] wikiFiles = this.wikiDirectory.listFiles();
        for (File f : wikiFiles) {
            System.out.println("Indexing file: " + f.getName() + "...");
            indexFile(f);
        }

        writer.close();
        index.close();
        System.out.println("---------------INDEX BUILT SUCCESSFULLY---------------\n\n");
    }

    /**
     * Parses a file containing Wikipedia pages and adds each page to the index.
     *
     * This creates Lucene Documents for each Wiki page and adds it to the index.
     * @param wikiFile      a text file containing multiple Wikipedia pages
     * @throws IOException  if a required file cannot be accessed
     */
    private void indexFile(File wikiFile) throws IOException {
        Scanner sc = new Scanner(wikiFile);

        // Fields of the Wikipedia page currently being parsed
        Document curDoc = null;
        int curDocId = 1;   // ID of the Lucene Document to be created for the page
        String curdocTitle = "";
        String curDocCategories = "";
        String curDocBody = "";

        while(sc.hasNextLine()) {
            String line = sc.nextLine();

            if(line.isBlank()) {
                continue;
            }

            if(isTitle(line)) {
                // Index the current Wiki page and create a Lucene Document for the next one
                if(curDoc != null) {
                    // curDoc is null for the first Document
                    indexDocument(curDoc, curdocTitle, curDocCategories, curDocBody);
                    curDocBody = "";
                }

                curDoc = new Document();
                curDoc.add(new StringField("docId", Integer.toString(++curDocId), Field.Store.YES));

                curdocTitle = line.substring(2, line.length() - 2);
                continue;
            }

            if(isCategoryList(line)) {
                curDocCategories = line.substring("CATEGORIES: ".length());
                continue;
            }

            if(isSubHeading(line)) {
                line = line.replace("=", "");
            }

            if(isAttachment(line)) {
                line = line.substring("FILE: ".length());
            }

            curDocBody += line;
        }
        // Index last Wiki page parsed
        indexDocument(curDoc, curdocTitle, curDocCategories, curDocBody);

        sc.close();
    }

    /**
     * Sets the fields of an Lucene Document and adds it to the Positional Index .
     *
     * Fields are added to the document such that the Index is a Positional Index.
     * @param doc           a Lucene Document for a Wiki page
     * @param docTitle      title of the Wiki page
     * @param docCategories categories that the Wiki page falls in
     * @param docBody       content of the Wiki page
     * @throws IOException  if a required file or directory cannot be accessed
     */
    private void indexDocument(Document doc, String docTitle, String docCategories, String docBody) throws IOException {
        // Field type for positional index
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        // Set fields
        doc.add(new Field("title", docTitle, fieldType));
        doc.add(new Field("categories", docCategories, fieldType));
        doc.add(new Field("body", docBody, fieldType));

        // Write the document to the Index
        this.writer.addDocument(doc);
    }

    /**
     * Checks whether a line in the Wiki page is alt text for an attachment
     * @param line  a line in the body of a Wiki page
     * @return      true if it is alt text, false otherwise
     */
    private boolean isAttachment(String line) {
        return line.startsWith("[[File:");
    }

    /**
     * Checks whether a line is the title of a Wiki page
     * @param line  a line from the Wiki files
     * @return      true if it is the title of a new Wiki page, false otherwise
     */
    private boolean isTitle(String line) {
        return !isAttachment(line) && line.startsWith("[[") && line.endsWith("]]");
    }

    /**
     * Checks whether a line is a list of categories for a Wiki page
     * @param line  a line from the Wiki files
     * @return      true if it is a list of categories, false otherwise
     */
    private boolean isCategoryList(String line) {
        return line.startsWith("CATEGORIES:");
    }

    /**
     * Checks whether a line in a Wiki page is a subheading
     * @param line  a line from the Wiki page
     * @return      true if it is a subheading, false otherwise
     */
    private boolean isSubHeading(String line) {
        return line.startsWith("=") && line.endsWith("=");
    }
}
