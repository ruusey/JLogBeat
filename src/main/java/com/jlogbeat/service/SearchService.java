package com.jlogbeat.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.jlogbeat.model.BlugeneLog;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SearchService {
	private static final String FILE_PATH = "D:/downloads/bgl2/bgl2";
	private final transient ThreadPoolTaskExecutor taskExecutor;
	private AnalyzingInfixSuggester suggestorLogs;
	private Boolean isReady = false;
	private Long curentOpStart;

	public SearchService(@Autowired ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@EventListener(ApplicationReadyEvent.class)
	private void readFileAndIndex() {
		SearchService.log.info("Starting blugene log search service.");
		List<BlugeneLog> logs = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(SearchService.FILE_PATH));
			String line;
			Integer lineCount = 0;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(" ");
				try {
					String[] logContent = Arrays.copyOfRange(split, 9, split.length);
					String logContentJoined = String.join(" ", logContent);
					BlugeneLog log = BlugeneLog.builder().logId(Long.parseLong(split[1])).date(split[2])
							.machineName(split[3]).dateTime(split[4]).code(split[6]).space(split[7]).level(split[8])
							.log(logContentJoined).build();

					logs.add(log);
					lineCount++;

				} catch (Exception e) {
					SearchService.log.error("Failed to parse line {}. Reason: {}", lineCount, e.getMessage());
				}
				if (lineCount == 200000) {
					break;
				}
			}
			br.close();
			SearchService.log.info("Loaded {} log entries into memory, Adding to Leucine Index.", lineCount);
			this.initLogsIndex(logs);

		}catch(Exception e) {
			SearchService.log.error("Failed to read logs file from path {}. Reason: {}", SearchService.FILE_PATH, e);
		}
	}

	private void initLogsIndex(List<BlugeneLog> accounts) throws Exception {
		this.resetOpTime();
		ByteBuffersDirectory autocompleteDirectory = new ByteBuffersDirectory();
		ByteBuffersDirectory directory = new ByteBuffersDirectory();
		SearchService.log.info("Initializing AnalyzingInfixSuggester...");
		this.suggestorLogs = new AnalyzingInfixSuggester(autocompleteDirectory, new StandardAnalyzer());
		try (IndexWriter directoryWriter = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()))) {
			List<Document> toIndex = new ArrayList<>();

			for (BlugeneLog line : accounts) {
				Document doc = new Document();
				doc.add(new TextField("id", line.getLogId() + "", Store.YES));
				doc.add(new TextField("machineName", line.getMachineName(), Store.YES));
				doc.add(new TextField("date", line.getDate(), Store.YES));
				doc.add(new TextField("dateTime", line.getDateTime(), Store.YES));

				doc.add(new TextField("code", line.getCode(), Store.YES));
				doc.add(new TextField("space", line.getSpace(), Store.YES));
				doc.add(new TextField("level", line.getLevel(), Store.YES));
				doc.add(new TextField("log", line.getLog(), Store.YES));

				toIndex.add(doc);

				HashSet<BytesRef> contexts = new HashSet<>();
				contexts.add(new BytesRef("log"));

				BytesRef payload = this.payload(line);
				//				String logSuggestion = String.join(" ", line.getLogId() + "", line.getMachineName(), line.getDateTime(),
				//						line.getCode(), line.getSpace(), line.getLevel() + line.getLog());
				this.suggestorLogs.add(
						new BytesRef(line.getLogId() + ""), contexts, 1, payload);
				this.suggestorLogs.add(
						new BytesRef(line.getMachineName()), contexts, 1, payload);
				this.suggestorLogs.add(new BytesRef(line.getDateTime()), contexts, 1, payload);
				this.suggestorLogs.add(new BytesRef(line.getCode()), contexts, 1, payload);
				this.suggestorLogs.add(new BytesRef(line.getSpace()), contexts, 1, payload);
				this.suggestorLogs.add(new BytesRef(line.getLevel()),
						contexts, 1, payload);
				this.suggestorLogs.add(new BytesRef(line.getLog()), contexts, 1, payload);

			}
			directoryWriter.addDocuments(toIndex);
			this.suggestorLogs.commit();
			this.suggestorLogs.refresh();

			directoryWriter.close();
			SearchService.log.info("Succesfully initialized Index{}", this.timeSinceStr());
		}
	}

	public List<Object> getUserSearchReccomendations(String txt) throws Exception {
		return this.suggestLogs(txt, 100);
	}

	public List<Object> suggestLogs(String term, Integer maxResults) throws IOException {
		this.resetOpTime();
		SearchService.log.info("Returning suggestions for Logs matching query '{}'", term);
		List<Lookup.LookupResult> lookup = this.suggestorLogs.lookup(term, false, maxResults);
		List<Object> suggestions = lookup.stream().map(SearchService::getPayload).collect(Collectors.toList());
		SearchService.log.info("Found {} suggestion(s){}", suggestions.size(), this.timeSinceStr());
		return suggestions;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getPayload(Lookup.LookupResult result) {
		try {
			BytesRef payload = result.payload;
			if (payload != null) {
				ByteArrayInputStream bis = new ByteArrayInputStream(payload.bytes);
				ObjectInputStream in = new ObjectInputStream(bis);
				T p = (T) in.readObject();
				bis.close();
				in.close();
				return p;
			}
			return null;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			throw new Error("Could not decode payload :(");
		}
	}

	public BytesRef payload(Object o) {
		BytesRef res = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(o);
			out.close();
			res = new BytesRef(bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			SearchService.log.error("Failed to convert object to byte[]... Does it implement Serilizable? {}",
					e.getMessage());
		}
		return res;
	}

	public Boolean getIsReady() {
		return this.isReady;
	}

	private void resetOpTime() {
		this.curentOpStart = Instant.now().toEpochMilli();
	}

	private String timeSinceStr() {
		return " in " + (Instant.now().toEpochMilli() - this.curentOpStart) + "ms ";
	}

	private String timeSinceStr(Long start) {
		return " in " + (Instant.now().toEpochMilli() - start) + "ms ";
	}
}
