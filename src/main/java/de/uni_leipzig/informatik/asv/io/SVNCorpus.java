/*
 * Copyright 2011 Arnim Bleier
 * Licensed under the GNU Lesser General Public License.
 * http://www.gnu.org/licenses/lgpl.html
 */

package de.uni_leipzig.informatik.asv.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 * @author <a href="mailto:arnim.bleier+hdp@gmail.com">Arnim Bleier</a>
 */
public class SVNCorpus extends ArrayList<Document> implements Corpus   {
	

	private static final long serialVersionUID = -8568648610437635401L;
	public int sizeVocabulary = 0;
	public int totalNumberOfWords = 0;

	
	
	public void read(InputStream is) {
		int length, word;
		SVNDocument d;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is,
					"UTF-8"));
			String line = null;
			while ((line = br.readLine()) != null) {
				try {
					String[] fields = line.split(" ");
					length = Integer.parseInt(fields[0]);
					d = new SVNDocument(length);
					for (int n = 0; n < length; n++) {
						String[] wordCounts = fields[n + 1].split(":");
						word = Integer.parseInt(wordCounts[0]);
						d.words[n] = word;
						d.counts[n] = Integer.parseInt(wordCounts[1]);
						d.total += Integer.parseInt(wordCounts[1]);
						if (word >= sizeVocabulary)
							sizeVocabulary = word + 1;
					}
					totalNumberOfWords += d.total;
					add(d);
				} catch (Exception e) {
					System.err.println(e.getMessage() + "\n");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void read(String filename) throws FileNotFoundException {
		read(new FileInputStream(filename));
	}


	@Override
	public int getVocabularySize() {
		return sizeVocabulary;
	}

	@Override
	public int getTotalNumberOfWords() {
		return totalNumberOfWords;
	}



}