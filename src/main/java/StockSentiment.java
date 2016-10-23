import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by tsudha on 10/19/16.
 */
public class StockSentiment {

    private FriendOracle friendOracle;

    public StockSentiment(FriendOracle friendOracle) {
        this.friendOracle = friendOracle;
    }

    /**
     * Get all the stock alerts corresponding to a user
     * @param myUserId - Id of the user
     * @return List of stock alerts sorted by rank
     */
    public List<String> getAlerts(String myUserId) {
        Map<String, Integer> stockRanks = new HashMap<>();

        DateTime lastWeek =  new DateTime().minusWeeks(1).withTimeAtStartOfDay();
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

        List<String> myFriends = friendOracle.getFriendsListForUser(myUserId);

        //Process all valid trades
        for(String friendId : myFriends) {
            List<String> trades = friendOracle.getTradeTransactionsForUser(friendId);
            for(String trade : trades) {
                processTrade(trade, lastWeek, dateTimeFormatter, stockRanks);
            }
        }

        List<Map.Entry<String, Integer>> stockRanksList = sortStockRanks(stockRanks);

        return buildAlertStrings(stockRanksList);
    }

    //Transform valid stock ranks and sort
    protected List<Map.Entry<String, Integer>> sortStockRanks(Map<String, Integer> stockRanks) {
        //Filter out all stocks with non-0 rank
        List<Map.Entry<String, Integer>> stockRanksList = stockRanks.entrySet().stream()
                .filter((rank) -> (rank.getValue() != 0))
                .collect(Collectors.toList());

        //First Sort stock-ranks based on stock name to make the result deterministic
        stockRanksList.sort((e1, e2) -> e1.getKey().compareTo(e2.getKey()));

        //Sort stock-ranks based on absolute rank in decreasing order
        stockRanksList.sort((e1, e2) -> {
            int absRank1 = Math.abs(e1.getValue());
            int absRank2 = Math.abs(e2.getValue());
            return new Integer(absRank2).compareTo(new Integer(absRank1));
        });

        return stockRanksList;
    }

    //Transform stockRanksList to Alert strings
    protected List<String> buildAlertStrings(List<Map.Entry<String, Integer>> stockRanksList) {
        return stockRanksList.stream().map((stockRank) -> {
            StringBuilder builder = new StringBuilder();
            builder.append(Math.abs(stockRank.getValue()));
            builder.append(",");
            builder.append((stockRank.getValue() > 0) ? "BUY" : "SELL");
            builder.append(",");
            builder.append(stockRank.getKey());
            return builder.toString();
        }).collect(Collectors.toList());
    }

    // process trade and populate stock-ranks hashmap
    protected void processTrade(String trade, DateTime lastWeek,
                              DateTimeFormatter dateTimeFormatter, Map<String, Integer> stockRanks) {
       String[] tradeArray =  trade.split(",");
        String tradeDate = tradeArray[0];
        int tradeType = tradeArray[1].equals("BUY") ? 1 : -1;
        String stock = tradeArray[2];
        DateTime tradeDateTime = dateTimeFormatter.parseDateTime(tradeDate);
        if(tradeDateTime.isAfter(lastWeek)) {
            if(stockRanks.containsKey(stock)) {
                stockRanks.put(stock, stockRanks.get(stock) + tradeType);
            } else {
                stockRanks.put(stock, tradeType);
            }
        }
    }
}


