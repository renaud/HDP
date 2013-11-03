package de.uni_leipzig.informatik.asv.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.junit.Test;

public class CLDACorpusTest {

	@Test
	public void test() throws Exception {
		InputStream is = CLDACorpus.class.getResourceAsStream("corpus1.lda-c");
		CLDACorpus corpus = new CLDACorpus(is);

		int[][] docs = corpus.getDocuments();
		assertEquals(3, docs.length);
		assertEquals(5, corpus.getVocabularySize());

		// System.out.println(Arrays.toString(docs[0]));
		assertEquals(3, docs[0].length);
		assertArrayEquals(new int[] { 0, 1, 2 }, docs[0]);
		assertEquals(5, docs[1].length);
		assertArrayEquals(new int[] { 0, 1, 3, 3, 3 }, docs[1]);
		assertEquals(4, docs[2].length);
		assertArrayEquals(new int[] { 0, 0, 4, 4 }, docs[2]);
	}

	@Test
	public void testNDocs() throws Exception {
		InputStream is = CLDACorpus.class.getResourceAsStream("corpus1.lda-c");
		CLDACorpus corpus = new CLDACorpus(is, 2);
		assertEquals(2, corpus.getDocuments().length);
	}
}
