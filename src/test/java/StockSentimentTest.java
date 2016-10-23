import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class StockSentimentTest {

    private FriendOracle friendOracle;
    private StockSentiment stockSentiment;

    @Before
    public void setup() throws Exception {
        friendOracle = mock(FriendOracle.class);
        stockSentiment = new StockSentiment(friendOracle);
    }

    @Test
    public void testSortStockRanks() throws Exception {
        Map<String, Integer> stockRanks = new HashMap<>();

        stockRanks.put("GOOG", 4);
        stockRanks.put("GRPN", -5);
        stockRanks.put("AAPL", 5);
        stockRanks.put("CRM", -2);
        stockRanks.put("TWTR", 3);
        stockRanks.put("AMZN", 0);

        List<Map.Entry<String, Integer>> stockRankList = stockSentiment.sortStockRanks(stockRanks);

        assertEquals(stockRankList.size(), 5);
        assertEquals(stockRankList.get(0).getKey(), "AAPL");
        assertEquals(stockRankList.get(0).getValue(), Integer.valueOf(5));

        assertEquals(stockRankList.get(1).getKey(), "GRPN");
        assertEquals(stockRankList.get(1).getValue(), Integer.valueOf(-5));

        assertEquals(stockRankList.get(4).getKey(), "CRM");
        assertEquals(stockRankList.get(4).getValue(), Integer.valueOf(-2));
    }

    @Test
    public void testBuildAlertStrings() throws Exception {
        Map<String, Integer> stockRanks = new HashMap<>();
        stockRanks.put("GOOG", 5);
        stockRanks.put("AMZN", -3);

        List<String> alerts = stockSentiment.buildAlertStrings(new ArrayList<>(stockRanks.entrySet()));
        assertEquals(alerts.get(0), "5,BUY,GOOG");
        assertEquals(alerts.get(1), "3,SELL,AMZN");
    }

    @Test
    public void testProcessTrade() throws Exception {
        DateTime lastWeek =  new DateTime().minusWeeks(1).withTimeAtStartOfDay();
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        Map<String, Integer> stockRanks = new HashMap<String, Integer>();;

        // old trade shouldnt add anything to the map
        String oldtrade1 = "2016-01-01,BUY,GOOG";
        stockSentiment.processTrade(oldtrade1, lastWeek, dateTimeFormatter, stockRanks);
        assertFalse(stockRanks.containsKey("GOOG"));


        // 2 goog BUY trades
        String validGoogTrade1 = lastWeek.plusDays(1).toString(dateTimeFormatter) + ",BUY,GOOG";
        stockSentiment.processTrade(validGoogTrade1, lastWeek, dateTimeFormatter, stockRanks);
        String validGoogTrade2 = lastWeek.plusDays(1).toString(dateTimeFormatter) + ",BUY,GOOG";
        stockSentiment.processTrade(validGoogTrade2, lastWeek, dateTimeFormatter, stockRanks);
        assertTrue(stockRanks.containsKey("GOOG"));
        assertEquals(stockRanks.get("GOOG"), Integer.valueOf(2));

        // 1 amzn BUY and 3 amzn SELL
        String validAmznTrade1 = lastWeek.plusDays(1).toString(dateTimeFormatter) + ",BUY,AMZN";
        stockSentiment.processTrade(validAmznTrade1, lastWeek, dateTimeFormatter, stockRanks);
        String validAmznTrade2 = lastWeek.plusDays(1).toString(dateTimeFormatter) + ",SELL,AMZN";
        stockSentiment.processTrade(validAmznTrade2, lastWeek, dateTimeFormatter, stockRanks);
        String validAmznTrade3 = lastWeek.plusDays(1).toString(dateTimeFormatter) + ",SELL,AMZN";
        stockSentiment.processTrade(validAmznTrade3, lastWeek, dateTimeFormatter, stockRanks);
        String validAmznTrade4 = lastWeek.plusDays(1).toString(dateTimeFormatter) + ",SELL,AMZN";
        stockSentiment.processTrade(validAmznTrade4, lastWeek, dateTimeFormatter, stockRanks);
        assertTrue(stockRanks.containsKey("AMZN"));
        assertEquals(stockRanks.get("AMZN"), Integer.valueOf(-2));
    }

    @Test
    public void testGetAlerts() throws Exception {

        List<String> user1Trades = new ArrayList<>();
        user1Trades.add("");
        user1Trades.add("");

        List<String> user2Trades = new ArrayList<>();
        user2Trades.add("");
        user2Trades.add("");
        user2Trades.add("");
        user2Trades.add("");

        Map<String, List<String>> trades = new HashMap<>();

        DateTime baseDay =  new DateTime().minusWeeks(1).withTimeAtStartOfDay();
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

        trades.put("user1", Arrays.asList("2014-01-01,BUY,GOOG",
                baseDay.plusDays(1).toString(dateTimeFormatter) + ",BUY,AMZN",
                baseDay.plusDays(2).toString(dateTimeFormatter) + ",SELL,BABA",
                baseDay.plusDays(4).toString(dateTimeFormatter) + ",SELL,GRPN",
                baseDay.plusDays(6).toString(dateTimeFormatter) + ",SELL,GRPN",
                baseDay.plusDays(6).toString(dateTimeFormatter) + ",SELL,AAPL",
                baseDay.plusDays(6).toString(dateTimeFormatter) + ",BUY,CRM"
        ));

        trades.put("user2", Arrays.asList(
                baseDay.plusDays(1).toString(dateTimeFormatter) + ",BUY,GOOG",
                baseDay.plusDays(2).toString(dateTimeFormatter) + ",BUY,BABA",
                baseDay.plusDays(2).toString(dateTimeFormatter) + ",SELL,GRPN",
                baseDay.plusDays(6).toString(dateTimeFormatter) + ",SELL,AAPL",
                baseDay.plusDays(6).toString(dateTimeFormatter) + ",BUY,CRM"
        ));

        FriendOracle myFriendOracle = new FriendOracle() {
            @Override
            public List<String> getFriendsListForUser(String userId) {
                return Arrays.asList("user1", "user2");
            }

            @Override
            public List<String> getTradeTransactionsForUser(String userId) {
                return trades.get(userId);
            }
        };

        stockSentiment = new StockSentiment(myFriendOracle);
        List<String> alerts = stockSentiment.getAlerts("myUserId");
        assertEquals(alerts.size(), 5);
        assertEquals(alerts.get(0), "3,SELL,GRPN");
        assertEquals(alerts.get(1), "2,SELL,AAPL");
        assertEquals(alerts.get(2), "2,BUY,CRM");
        assertEquals(alerts.get(3), "1,BUY,AMZN");
        assertEquals(alerts.get(4), "1,BUY,GOOG");
    }
}