package de.uni_leipzig.informatik.asv.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a corpus in LDA-C format:
 * 
 * <pre>
 *  [M] [term_1]:[count] [term_2]:[count] ...  [term_N]:[count]
 * </pre>
 * 
 * where [M] is the number of unique terms in the document, and the [count]
 * associated with each term is how many times that term appeared in the
 * document.
 */
public class CLDACorpus {

	private int[][] documents;
	private int vocabularySize = 0;

	/**
	 * Reads all documents from a corpus
	 */
	public CLDACorpus(InputStream is) throws IOException {
		this(is, Integer.MAX_VALUE);
	}

	/**
	 * Reads up to
	 * 
	 * @param nrDocs
	 *            documents.
	 */
	public CLDACorpus(InputStream is, int nrDocs) throws IOException {
		int length, word, counts;
		List<List<Integer>> docList = new ArrayList<List<Integer>>();
		List<Integer> doc;
		BufferedReader br = new BufferedReader(new InputStreamReader(is,
				"UTF-8"));
		String line = null;
		while ((line = br.readLine()) != null && docList.size() < nrDocs) {
			try {
				doc = new ArrayList<Integer>();
				String[] fields = line.split(" ");
				length = Integer.parseInt(fields[0]);
				for (int n = 0; n < length; n++) {
					String[] wordCounts = fields[n + 1].split(":");
					word = Integer.parseInt(wordCounts[0]);
					counts = Integer.parseInt(wordCounts[1]);
					for (int i = 0; i < counts; i++)
						doc.add(word);
					if (word >= vocabularySize)
						vocabularySize = word + 1;
				}
				docList.add(doc);
			} catch (Exception e) {
				System.err.println(e.getMessage() + "\n");
			}
		}
		documents = new int[docList.size()][];
		for (int j = 0; j < docList.size(); j++) {
			doc = docList.get(j);
			documents[j] = new int[doc.size()];
			for (int i = 0; i < doc.size(); i++) {
				documents[j][i] = doc.get(i);
			}
		}
	}

	public int[][] getDocuments() {
		return documents;
	}

	public int getVocabularySize() {
		return vocabularySize;
	}
}
