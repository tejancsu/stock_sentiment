import java.util.List;

/**
 * Created by tsudha on 10/19/16.
 */
public interface FriendOracle {
    List<String> getFriendsListForUser(String userId);
    List<String> getTradeTransactionsForUser(String userId);
}
