/*
 * Copyright 2011 Arnim Bleier, Andreas Niekler and Patrick Jaehnichen
 * Licensed under the GNU Lesser General Public License.
 * http://www.gnu.org/licenses/lgpl.html
 */
package de.uni_leipzig.informatik.asv.hdp;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import de.uni_leipzig.informatik.asv.utils.CLDACorpus;

/**
 * Hierarchical Dirichlet Processes Chinese Restaurant Franchise Sampler
 * 
 * For more information on the algorithm see: Hierarchical Bayesian
 * Nonparametric Models with Applications. Y.W. Teh and M.I. Jordan. Bayesian
 * Nonparametrics, 2010. Cambridge University Press.
 * http://www.gatsby.ucl.ac.uk/~ywteh/research/npbayes/TehJor2010a.pdf
 * 
 * For other known implementations see README.txt
 * 
 * @author <a href="mailto:arnim.bleier+hdp@gmail.com">Arnim Bleier</a>
 * @author renaud.richardet@epfl.ch
 */
public class HDPGibbsSampler {

	public static final double ALPHA = 1.0;
	public static final double BETA = 0.5; // default only
	public static final double GAMMA = 1.5;

	private final Random random = new Random();
	private double[] p;
	private double[] f;

	/** |V| */
	private int sizeOfVocabulary;
	/** |W| */
	private int totalNumberOfWords;
	/** |K| */
	private int numberOfTopics = 1;
	/** |T|?? */
	private int totalNumberOfTables;

	/** D x {@link Doc}; = |D| x */
	private Doc[] docs;
	private int[] numberOfTablesByTopic;
	private int[] wordCountByTopic;
	/** K x |V| */
	private int[][] wordCountByTopicAndTerm;

	/**
	 * Initially randomly assign the words to tables and topics.
	 * 
	 * @param corpus
	 *            {@link CLDACorpus#getDocuments()} on which to fit the model
	 * @param V
	 *            the size of the vocabulary
	 */
	public void addInstances(int[][] documentsInput, int V) {
		sizeOfVocabulary = V;
		totalNumberOfWords = 0;
		docs = new Doc[documentsInput.length];
		for (int d = 0; d < documentsInput.length; d++) {
			docs[d] = new Doc(documentsInput[d], d);
			totalNumberOfWords += documentsInput[d].length;
		}

		p = new double[20];
		f = new double[20];
		numberOfTablesByTopic = new int[numberOfTopics + 1];
		wordCountByTopic = new int[numberOfTopics + 1];
		wordCountByTopicAndTerm = new int[numberOfTopics + 1][];
		for (int k = 0; k <= numberOfTopics; k++)
			// variable initialization already done
			wordCountByTopicAndTerm[k] = new int[sizeOfVocabulary];
		for (int k = 0; k < numberOfTopics; k++) {
			Doc doc = docs[k];
			for (int wi = 0; wi < doc.documentLength; wi++)
				updateWord(doc.id, wi, 0, k);
		} // all topics have now one document
		for (int di = numberOfTopics /* ! */; di < docs.length; di++) {
			Doc doc = docs[di];
			int k = random.nextInt(numberOfTopics);
			for (int wi = 0; wi < doc.documentLength; wi++)
				updateWord(doc.id, wi, 0, k);
		} // the words in the remaining documents are now assigned too, at rnd
	}

	/**
	 * Add a word to the bookkeeping
	 * 
	 * @param di
	 *            the id of the document the word belongs to
	 * @param wi
	 *            the index of the word
	 * @param ti
	 *            the table to which the word is assigned to
	 * @param ki
	 *            the topic to which the word is assigned to
	 */
	private void updateWord(int di, int wi, int ti, int ki) {
		Doc d = docs[di];
		d.words[wi].tableAssignment = ti;
		d.wordCountByTable[ti]++;
		wordCountByTopic[ki]++;
		wordCountByTopicAndTerm[ki][d.words[wi].termIndex]++;
		if (d.wordCountByTable[ti] == 1) { // a new table is created
			d.numberOfTables++;
			d.tableToTopic[ti] = ki;
			totalNumberOfTables++;
			numberOfTablesByTopic[ki]++;
			d.tableToTopic = ensureCapacity(d.tableToTopic, d.numberOfTables);
			d.wordCountByTable = ensureCapacity(d.wordCountByTable,
					d.numberOfTables);
			if (ki == numberOfTopics) { // a new topic is created
				numberOfTopics++;
				numberOfTablesByTopic = ensureCapacity(numberOfTablesByTopic,
						numberOfTopics);
				wordCountByTopic = ensureCapacity(wordCountByTopic,
						numberOfTopics);
				wordCountByTopicAndTerm = add(wordCountByTopicAndTerm,
						new int[sizeOfVocabulary], numberOfTopics);
			}
		}
	}

	/**
	 * Trains (fits) the model by Gibbs sampling.
	 * 
	 * @param shuffleLag
	 *            at which interval to shuffle the documents
	 * @param maxIter
	 *            number of iterations to run
	 * @param saveLag
	 *            save interval
	 * @param log
	 *            to write to
	 */
	public void train(int shuffleLag, int maxIter, PrintStream log)
			throws IOException {
		int start = (int) (currentTimeMillis() / 1000);// some stats
		log.println("time\titer\t#topics\t#tables");

		for (int iter = 0; iter < maxIter; iter++) {

			if ((shuffleLag > 0) && (iter > 0) && (iter % shuffleLag == 0))
				shuffle();
			gibbsSampling();

			int time = (int) (System.currentTimeMillis() / 1000) - start;
			log.println(time + "\t" + iter + "\t" + numberOfTopics + "\t"
					+ totalNumberOfTables);
		}
		log.println("training complete");
	}

	/** Gibbs sampling, then defragment */
	private void gibbsSampling() {
		int ti;
		for (int di = 0; di < docs.length; di++) {
			for (int wi = 0; wi < docs[di].documentLength; wi++) {
				removeWord(di, wi); // remove the word i from the doc
				ti = sampleTable(di, wi);
				if (ti == docs[di].numberOfTables) { // new Table
					int ki = sampleTopic(); // sample this word's Topic
					updateWord(di, wi, ti, ki);
				} else { // existing Table
					updateWord(di, wi, ti, docs[di].tableToTopic[ti]);
				}
			}
		}
		defragment();
	}

	/**
	 * Decide at which topic the table should be assigned to
	 * 
	 * @return the index of the topic
	 */
	private int sampleTopic() {
		double u, pSum = 0.0;
		int k;
		p = ensureCapacity(p, numberOfTopics);
		for (k = 0; k < numberOfTopics; k++) {
			pSum += numberOfTablesByTopic[k] * f[k];
			p[k] = pSum;
		}
		pSum += GAMMA / sizeOfVocabulary;
		p[numberOfTopics] = pSum;
		u = random.nextDouble() * pSum;
		for (k = 0; k <= numberOfTopics; k++)
			if (u < p[k])
				break;
		return k;
	}

	/**
	 * Decide at which table the word should be assigned to
	 * 
	 * @param di
	 *            the index of the document (of the current word to sample)
	 * @param wi
	 *            the index of the current word
	 * @return the index of the table
	 */
	private int sampleTable(int di, int wi) {
		int ti;
		double vb = sizeOfVocabulary * BETA, u;
		Doc docState = docs[di];
		f = ensureCapacity(f, numberOfTopics);
		p = ensureCapacity(p, docState.numberOfTables);

		double fNew = sampleTable1(docState, wi, vb);
		double pSum = sampleTable2(docState);

		// Probability for t = tNew
		pSum += ALPHA * fNew / (totalNumberOfTables + GAMMA);
		p[docState.numberOfTables] = pSum;
		u = random.nextDouble() * pSum;
		for (ti = 0; ti <= docState.numberOfTables; ti++)
			if (u < p[ti])
				break; // decided which table the word i is assigned to
		return ti;
	}

	/**
	 * @param doc
	 * @param wi
	 * @param vb
	 *            sizeOfVoc * BETA
	 * @return fNew
	 */
	private double sampleTable1(Doc doc, int wi, double vb) {
		double fNew = GAMMA / sizeOfVocabulary;
		for (int ki = 0; ki < numberOfTopics; ki++) {
			f[ki] = (wordCountByTopicAndTerm[ki][doc.words[wi].termIndex] + BETA)
					/ (wordCountByTopic[ki] + vb);
			fNew += numberOfTablesByTopic[ki] * f[ki];
		}
		return fNew;
	}

	private double sampleTable2(Doc doc) {
		double pSum = 0.0;
		for (int ti = 0; ti < doc.numberOfTables; ti++) {
			if (doc.wordCountByTable[ti] > 0)
				pSum += doc.wordCountByTable[ti] * f[doc.tableToTopic[ti]];
			p[ti] = pSum;
		}
		return pSum;
	}

	/**
	 * Removes a word from the bookkeeping.
	 * 
	 * @param di
	 *            the id of the document the word belongs to
	 * @param wi
	 *            the index of the word
	 */
	private void removeWord(int di, int wi) {
		Doc doc = docs[di];
		int ti = doc.words[wi].tableAssignment;
		int ki = doc.tableToTopic[ti];
		doc.wordCountByTable[ti]--;
		wordCountByTopic[ki]--;
		wordCountByTopicAndTerm[ki][doc.words[wi].termIndex]--;
		if (doc.wordCountByTable[ti] == 0) { // table is removed
			totalNumberOfTables--;
			numberOfTablesByTopic[ki]--;
			doc.tableToTopic[ti]--;
		}
	}

	/** Removes topics from the bookkeeping that have no words assigned to */
	private void defragment() {
		int[] kOldToKNew = new int[numberOfTopics];
		int newNumberOfTopics = 0;
		for (int ki = 0; ki < numberOfTopics; ki++) {
			if (wordCountByTopic[ki] > 0) {
				kOldToKNew[ki] = newNumberOfTopics;
				swap(wordCountByTopic, newNumberOfTopics, ki);
				swap(numberOfTablesByTopic, newNumberOfTopics, ki);
				swap(wordCountByTopicAndTerm, newNumberOfTopics, ki);
				newNumberOfTopics++;
			}
		}
		numberOfTopics = newNumberOfTopics;
		for (int di = 0; di < docs.length; di++)
			docs[di].defragment(kOldToKNew);
	}

	/** Permute the ordering of documents and words in the bookkeeping */
	private void shuffle() {
		List<Doc> tmpDocs = asList(docs);
		Collections.shuffle(tmpDocs);
		docs = tmpDocs.toArray(new Doc[tmpDocs.size()]);
		for (int di = 0; di < docs.length; di++) {
			List<Word> tmpWords = asList(docs[di].words);
			Collections.shuffle(tmpWords);
			docs[di].words = tmpWords.toArray(new Word[tmpWords.size()]);
		}
	}

	private static void swap(int[] arr, int arg1, int arg2) {
		int t = arr[arg1];
		arr[arg1] = arr[arg2];
		arr[arg2] = t;
	}

	private static void swap(int[][] arr, int arg1, int arg2) {
		int[] t = arr[arg1];
		arr[arg1] = arr[arg2];
		arr[arg2] = t;
	}

	private static int[] ensureCapacity(int[] arr, int min) {
		int length = arr.length;
		if (min < length)
			return arr;
		int[] arr2 = new int[min * 2];
		for (int i = 0; i < length; i++)
			arr2[i] = arr[i];
		return arr2;
	}

	private static double[] ensureCapacity(double[] arr, int min) {
		int length = arr.length;
		if (min < length)
			return arr;
		double[] arr2 = new double[min * 2];
		for (int i = 0; i < length; i++)
			arr2[i] = arr[i];
		return arr2;
	}

	private static int[][] add(int[][] arr, int[] newElement, int index) {
		int length = arr.length;
		if (length <= index) {
			int[][] arr2 = new int[index * 2][];
			for (int i = 0; i < length; i++)
				arr2[i] = arr[i];
			arr = arr2;
		}
		arr[index] = newElement;
		return arr;
	}

	/** A text document */
	private class Doc {

		int id, documentLength, numberOfTables;
		int[] tableToTopic;
		int[] wordCountByTable;
		Word[] words;

		/**
		 * @param words
		 *            the {@link Word}s in that document
		 * @param id
		 *            document id. so far assigned within
		 *            {@link HDPGibbsSampler#addInstances()}
		 */
		Doc(int[] words, int id) {
			this.id = id;
			numberOfTables = 0;
			documentLength = words.length;
			this.words = new Word[documentLength];
			tableToTopic = new int[2];
			wordCountByTable = new int[2];
			for (int wi = 0; wi < documentLength; wi++)
				this.words[wi] = new Word(words[wi], -1);
		}

		public void defragment(int[] kOldToKNew) {
			int[] tOldToTNew = new int[numberOfTables];
			int t, newNumberOfTables = 0;
			for (t = 0; t < numberOfTables; t++) {
				if (wordCountByTable[t] > 0) {
					tOldToTNew[t] = newNumberOfTables;
					tableToTopic[newNumberOfTables] = kOldToKNew[tableToTopic[t]];
					swap(wordCountByTable, newNumberOfTables, t);
					newNumberOfTables++;
				} else
					tableToTopic[t] = -1;
			}
			numberOfTables = newNumberOfTables;
			for (int i = 0; i < documentLength; i++)
				words[i].tableAssignment = tOldToTNew[words[i].tableAssignment];
		}

	}

	/** The state of a word within a {@link Doc} */
	private class Word {

		int termIndex;
		int tableAssignment;

		/**
		 * @param wordIndex
		 *            index in the vocabulary
		 * @param tableAssignment
		 *            or -1 for no assignment
		 */
		Word(int wordIndex, int tableAssignment) {
			this.termIndex = wordIndex;
			this.tableAssignment = tableAssignment;
		}
	}

	public static void main(String[] args) throws IOException {

		// String corpusFile =
		// "/Volumes/HDD2/ren_data/dev_hdd/bluebrain/9_lda/topic_models_datasets/genia/genia.lda-c";
		// String corpusFile =
		// "/Volumes/HDD2/ren_data/dev_hdd/bluebrain/9_lda/topic_models_datasets/20_newsgroups/twenty_newsgroups.lda-c";
		String corpusFile = "/Users/richarde/dev/bluebrain/git/topic_models_datasets/20_newsgroups/preprocessing_2/20ng.lda-c";
		// String corpusFile =
		// "/Volumes/HDD2/ren_data/dev_hdd/bluebrain/9_lda/topic_models_datasets/pubmed_abstracts_100k.ldac-txtbag";
		String outFile = "topics.dat";
		String outFile2 = "topics2.dat";// TODO

		CLDACorpus corpus = new CLDACorpus(new FileInputStream(corpusFile));
		HDPGibbsSampler hdp = new HDPGibbsSampler();
		hdp.addInstances(corpus.getDocuments(), corpus.getVocabularySize());

		System.out.println("sizeOfVocabulary = " + hdp.sizeOfVocabulary);
		System.out.println("totalNumberOfWords = " + hdp.totalNumberOfWords);
		System.out.println("NumberOfDocs = " + hdp.docs.length);

		hdp.train(0, 50, System.out);

		// print
		PrintStream file = new PrintStream(outFile);
		for (int k = 0; k < hdp.numberOfTopics; k++) {
			for (int w = 0; w < hdp.sizeOfVocabulary; w++)
				file.format("%05d ", hdp.wordCountByTopicAndTerm[k][w]);
			file.println();
		}
		file.close();

		file = new PrintStream(outFile2);
		file.println("d w z t");
		int t, docID;
		for (int d = 0; d < hdp.docs.length; d++) {
			Doc doc = hdp.docs[d];
			docID = doc.id;
			for (int i = 0; i < doc.documentLength; i++) {
				t = doc.words[i].tableAssignment;
				file.println(docID + " " + doc.words[i].termIndex + " "
						+ doc.tableToTopic[t] + " " + t);
			}
		}
		file.close();
	}
}