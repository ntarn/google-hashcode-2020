import java.io.*;
import java.util.*;

class Solution {
  private static int numVideos;
  private static int numEndpoints;
  private static int numRequestDescs;
  private static int numCaches;
  private static int cacheSize;
  // Array of video sizes.
  private static int[] videoSizes;
  // Map from endpoint ID to the latency between the endpoint and the data center.
  private static HashMap<Integer, Integer> endpointCenterLatencies = new HashMap<>();
  // Map from endpoint ID to a map from ID of a cache connected to the endpoint to latency between them.
  private static HashMap<Integer, HashMap<Integer, Integer>> endpointCacheLatencies = new HashMap<>();
  // Map from video ID to a HashMap where the key is endpoint ID, value is request numbers.
  private static HashMap<Integer, HashMap<Integer, Integer>> videoEndpoints = new HashMap<>();

  private static int[] cacheSizes;
  private static int numCachesUsed;
  // Map from cache ID to the videos saved in it.
  private static HashMap<Integer, ArrayList<Integer>> cacheToVideos = new HashMap<>();

  public static void main(String[] args) {
    ArrayList<String> strings = new ArrayList<String>();
    strings.add("Hello, World!");
    strings.add("Please put code below");
    for (String string : strings) {
      System.out.println(string);
    }
    
    readData();
    cacheVideos();
  }

  public static void readData() {
    Scanner scanner;
    try {
      scanner = new Scanner(new File("./data/vloggers_of_the_world.in"));

      String[] firstLine = scanner.nextLine().split(" ");
      numVideos = Integer.parseInt(firstLine[0]);
      numEndpoints = Integer.parseInt(firstLine[1]);
      numRequestDescs = Integer.parseInt(firstLine[2]);
      numCaches = Integer.parseInt(firstLine[3]);
      cacheSize = Integer.parseInt(firstLine[4]);

      String[] secondLine = scanner.nextLine().split(" ");
      videoSizes = new int[numVideos];
      for (int i = 0; i < numVideos; i++) {
        videoSizes[i] = Integer.parseInt(secondLine[i]);
      }

      String[] line;
      for (int i = 0; i < numEndpoints; i++) {
        line = scanner.nextLine().split(" ");
        endpointCenterLatencies.put(i, Integer.valueOf(line[0]));

        int numCaches = Integer.parseInt(line[1]);
        HashMap<Integer, Integer> cacheLatencies = new HashMap<>();
        for (int j = 0; j < numCaches; j++) {
          line = scanner.nextLine().split(" ");
          cacheLatencies.put(j, Integer.valueOf(line[1])); 
        }
        endpointCacheLatencies.put(i, cacheLatencies);
      }

      for (int i = 0; i < numRequestDescs; i++) {
        line = scanner.nextLine().split(" ");
        Integer videoId = Integer.valueOf(line[0]);
        if (videoEndpoints.containsKey(videoId)) {
          videoEndpoints.get(videoId).put(Integer.valueOf(line[1]), Integer.valueOf(line[2]));
        } else {
          HashMap<Integer, Integer> endpointRequestNums = new HashMap<>();
          endpointRequestNums.put(Integer.valueOf(line[1]), Integer.valueOf(line[2]));
          videoEndpoints.put(videoId, endpointRequestNums);
        }
      }
    } catch (FileNotFoundException ex) {
      System.out.println("err");
    }
  }
  
  public static void cacheVideos() {
    // 1. Sum all video requests in array videoRequests across all endpoints
    int videoRequests[] = new int[numVideos]; 
    for (Integer i : videoEndpoints.keySet()) {
      for(Integer j : videoEndpoints.get(i).keySet()) {
        videoRequests[i] = videoRequests[i] + videoEndpoints.get(i).get(j);
      }
    }

    cacheSizes = new int[numCaches];
    Arrays.fill(cacheSizes, cacheSize);

    while (true) {
      // 2. Get the video with the most requests that has not been put in a cache. (Alternatively, do a   
      // combination of number of requests & video size.)
      int maxValue = videoRequests[0];
      int maxRequestsVideoId = 0;
      for (int i = 0; i < videoRequests.length; i++) { 
        if (videoRequests[i] > maxValue){ 
          maxValue = videoRequests[i]; 
          maxRequestsVideoId = i;
        }
      }
      if (maxValue == -1) { // When all videos are put in caches, don't continue. TODO: change this.
        break;
      }
      videoRequests[maxRequestsVideoId] = -1;
      //System.out.println("videoid" + maxRequestsVideoId);
      cacheVideo(maxValue, maxRequestsVideoId);

    }
    
    writeOutputs();
  }

  public static void cacheVideo(int maxValue, int maxRequestsVideoId) {
    int cacheScores[] = new int[numCaches];
    
    for (int endpointId = 0; endpointId < numEndpoints; endpointId++) {
      HashMap<Integer, Integer> cacheLatencies = endpointCacheLatencies.get(endpointId);
      for(Integer cacheIndex : cacheLatencies.keySet()) {
        // 2.5 Check to see if the video fits in the caches for each endpoint (on current endpoint)
        // 3. Calculate the cache scores and get the cache that has the greatest difference (summed)
        // for each endpoint (dataCenterLatency - minCacheAvailableLatency) * numRequests 
        if (videoSizes[maxRequestsVideoId] <= cacheSizes[cacheIndex] &&       
            videoEndpoints.containsKey(maxRequestsVideoId)) { 
          //System.out.println("videosize: " + videoSizes[maxRequestsVideoId] + " for: " + maxRequestsVideoId);
          if (videoEndpoints.get(maxRequestsVideoId).containsKey(endpointId)){
            cacheScores[cacheIndex] = cacheScores[cacheIndex] + 
                (endpointCenterLatencies.get(endpointId) - endpointCacheLatencies.get(endpointId).get(cacheIndex)) * 
                videoEndpoints.get(maxRequestsVideoId).get(endpointId);
          }
        } else {
          cacheScores[cacheIndex] = Integer.MIN_VALUE;
        }
      }
    }
    // Get the cache that has the greatest difference (summed)
    int maxCacheScore = Integer.MIN_VALUE;
    int maxCacheIndex = 0;
    for (int i=0; i < cacheScores.length; i++){ 
      if (videoSizes[maxRequestsVideoId] <= cacheSizes[i] && cacheScores[i] > maxCacheScore){ 
        maxCacheScore = cacheScores[i]; 
        maxCacheIndex= i;
      }
    }
    //System.out.println("index: " + maxCacheIndex + " size of cache:" +  cacheSizes[maxCacheIndex]);
    if (videoSizes[maxRequestsVideoId] > cacheSizes[maxCacheIndex]) {
      return; 
    }
    // 3.5 Put the video in the cache that has the greatest score.
    ArrayList<Integer> videos = cacheToVideos.get(maxCacheIndex); //change
    if (videos != null) {
      videos.add(maxRequestsVideoId);
      cacheToVideos.put(maxCacheIndex, videos);
    } else {
      videos = new ArrayList<>();
      videos.add(maxRequestsVideoId);
      cacheToVideos.put(maxCacheIndex, videos);
    }
    //System.out.println("before:" + cacheSizes[maxCacheIndex]);
    // 4. Mark the cache for the space taken
    cacheSizes[maxCacheIndex] = cacheSizes[maxCacheIndex] - videoSizes[maxRequestsVideoId];
    //System.out.println("after:" + cacheSizes[maxCacheIndex]);
  }

  public static void writeOutputs() {
    //System.out.println(cacheToVideos);
    System.out.println(cacheToVideos.size());
    for (Integer cacheId : cacheToVideos.keySet()) {
      System.out.print(cacheId);
      ArrayList<Integer> videoList = cacheToVideos.get(cacheId);
      for (Integer videoId : videoList) {
        System.out.print(" " + videoId);
      }
      System.out.print('\n');
    }
  }
}


/** 
 * Go through the videos with the descending order of their overall requset numbers; for each combination of available cache and endpoint that requests this video, calculate the scores by `(dataCenterLatency - cacheLatency) * numRequests`, and save the current video to the cache that has largested score. Mark the cache for the space taken.
 */