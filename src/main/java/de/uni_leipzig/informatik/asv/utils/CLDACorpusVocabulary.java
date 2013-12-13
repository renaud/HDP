package de.uni_leipzig.informatik.asv.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Reads a corpus vocabulary, one word per line. First line corresponds to w[0].<br/>
 * Skips lines starting with ##
 * 
 * @author renaud@apache.org
 */
public class CLDACorpusVocabulary {

	private String[] vocabArr;

	public String[] load(InputStream is) throws IOException {
		List<String> vocab = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(is,
				"UTF-8"));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (!line.startsWith("##"))
				vocab.add(line.trim());
		}
		this.vocabArr = vocab.toArray(new String[vocab.size()]);
		return vocabArr;
	}

	public String getWord(int idx) {
		return vocabArr[idx];
	}

	public String getDocument(int[] indexes) {

		// compact
		Map<Integer, Integer> words = new HashMap<Integer, Integer>();

		for (int i : indexes) {
			if (words.containsKey(i)) {
				words.put(i, words.get(i) + 1);
			} else {
				words.put(i, 1);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (Entry<Integer, Integer> entry : words.entrySet()) {
			sb.append(getWord(entry.getKey()) + "[" + entry.getValue() + "] ");
		}
		return sb.toString().trim();
	}
}
