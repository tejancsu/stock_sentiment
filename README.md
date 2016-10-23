The main code is in ```src/main/java/StockSentiment.java```

Test code is in ```test/java/StockSentimentTest.java```

Algorithm:
- Get all the trades for each friend
- Parse each valid trade and update the stockRankMap which is a hashmap (ticker,rank).
- Sort the key,value pairs in the stockRankMap.

Time and space complexities:
- m : number of friends
- n : avg number of trades of per friend
- k : number of distinct stock tickers bought by friends

Space - O(k)

Time - O(mn) + O(klogk)

To run the tests please run :
```mvn test```
