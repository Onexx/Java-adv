package info.kgeorgiy.ja.Zaitsev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private static final int DEFAULT_DOWNLOADERS = 4;
    private static final int DEFAULT_EXTRACTORS = 4;
    private static final int DEFAULT_PERHOST = 1;
    private static final int DEFAULT_DEPTH = 1;
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;

    @SuppressWarnings("unused")
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> used = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        queue.add(url);
        used.add(url);
        int levelSync = 1;
        Phaser phaser = new Phaser(1);

        while (!queue.isEmpty()) {
            String q = queue.poll();
            levelSync--;

            final Runnable downloadTask = () -> {
                try {
                    final Document document = downloader.download(q);

                    if (phaser.getPhase() + 1 >= depth) return;

                    final Runnable extract = () -> {
                        try {
                            for (String link : document.extractLinks()) {
                                if (used.add(link)) {
                                    queue.add(link);
                                }
                            }
                        } catch (IOException e) {
                            errors.put(q, e);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    };

                    phaser.register();
                    extractors.submit(extract);

                } catch (IOException e) {
                    errors.put(q, e);
                } finally {
                    phaser.arriveAndDeregister();
                }
            };

            phaser.register();
            downloaders.submit(downloadTask);

            if (levelSync == 0) {
                phaser.arriveAndAwaitAdvance();
                levelSync = queue.size();
            }
        }

        used.removeAll(errors.keySet());
        return new Result(new ArrayList<>(used), errors);
    }


    @Override
    public void close() {
        downloaders.shutdownNow();
        extractors.shutdownNow();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Incorrect arguments. Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }
        String url = args[0];
        int depth = parseArgOrDefault(args, 1, DEFAULT_DEPTH);
        int downloaders = parseArgOrDefault(args, 2, DEFAULT_DOWNLOADERS);
        int extractors = parseArgOrDefault(args, 3, DEFAULT_EXTRACTORS);
        int perHost = parseArgOrDefault(args, 4, DEFAULT_PERHOST);
        try {
            WebCrawler webCrawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost);
            webCrawler.download(url, depth);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int parseArgOrDefault(String[] args, int idx, int defaultValue) {
        if (idx < args.length) {
            try {
                return Integer.parseInt(args[idx]);
            } catch (NumberFormatException ignored) {
                System.err.println("Couldn't parse argument [" + args[idx] + "]. Using default value:" + defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }

}
