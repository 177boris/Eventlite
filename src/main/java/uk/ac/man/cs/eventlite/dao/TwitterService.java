package uk.ac.man.cs.eventlite.dao;

import java.util.List;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterService {
	
	private Twitter twitter;
	private static String API_KEY = "Z1SO6aBWwAh8wrccpRracxSSA";
	private static String API_KEY_SECRET ="ua9UwnzOkN2nAmId6jiFUVr8Odu2SA4qEE2mVemAeLHlQVyxVH";
	private static String ACCESS_TOKEN = "1157988383664353280-sYJFbDtKtX5Yd5xo1cNOspZ0b8rKdX";
	private static String ACCESS_TOKEN_SECRET ="DzgBjMPRiFytiiix66s4ykW0DxfALcHj0H4x1xz8zd9HZ";
	
	public TwitterService() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
          .setOAuthConsumerKey(API_KEY)
          .setOAuthConsumerSecret(API_KEY_SECRET)
          .setOAuthAccessToken(ACCESS_TOKEN)
          .setOAuthAccessTokenSecret(ACCESS_TOKEN_SECRET);
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
	}
    public List<Status> getTimeLine() throws TwitterException {
        
        Paging count = new Paging();
        count.setCount(5);     //last five tweets will be displayed
         
        return twitter.getHomeTimeline(count);
    }
   public boolean setTwitterInstance(String tweet) throws TwitterException{
	   try {
	    	Status status = twitter.updateStatus(tweet);
	    	return true;
	    }
	    catch(TwitterException e) {
	    	System.err.println(e.getErrorMessage());
	    	return false;
	    }

   }

    
}
