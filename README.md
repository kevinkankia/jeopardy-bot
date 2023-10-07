# Jeopardy Bot

### Description
Jeopardy Bot is an Apache Lucene based Information Retrieval system capable of playing Jeopardy.

The system uses a sampled set of ~300,000 Wikipedia pages and 100 relevant Jeopardy questions to assess its efficiency.

### Efficiency
The system provides 10 answers for each question, ranked by confidence levels.

Out of the 100 Jeopardy questions passed to the system, the expected answer was found in the top 10 results for 64 of them.

But the Jeopardy game only allows participants to give one answer for each question. The system was able to provide the expected answer as its first choice for 44 of the 100 questions. This gives us a P@1 (Precision at One) of 0.44.

### Output
The system first indexes all the text files containing multiple Wikipedia pages. Next, it prints the positions at which the expected answer was found for each question, and then prints the above mentioned statistics.

The output is formatted as such:
```
---------------BUILDING INDEX-------------------------
Indexing file: file-name...
.
.
.
Indexing file: file-name...
---------------INDEX BUILT SUCCESSFULLY---------------


---------------QUERY RESULTS--------------------------
Answer to Question 1: The Washington Post
Hit at position: 3

Answer to Question 2: Taiwan
Hit at position: 1

Answer to Question 3: The Wall Street Journal
Hit at position: 1
.
.
.
Answer to Question 98: Souvlaki
Hit at position: 1

Answer to Question 99: 3M

Answer to Question 100: Robert Downey, Jr.
Hit at position: 1
---------------STATISTICS-----------------------------
Docs in position 1: 44
Docs in position 2: 8
Docs in position 3: 6
Docs in position 4: 2
Docs in position 5: 1
Docs in position 6: 1
Docs in position 7: 1
Docs in position 8: 0
Docs in position 9: 1
Docs in position 10: 0

Hits in top 10 Documents: 64
P@1: 0.44
```
