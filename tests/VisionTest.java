import com.example.froggypilot.RestaurantVision;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** JVM-only smoke tests: all training snapshots can be parsed without crashes. */
public final class VisionTest {
    public static void main(String[] args) throws Exception {
        RestaurantVision engine=new RestaurantVision(new RestaurantVision.Options());
        File folder=new File("tests/frames");
        int count=0,coin=0,tutorial=0;
        for(File file:folder.listFiles()) {
            if(!file.getName().endsWith(".jpg") && !file.getName().endsWith(".png"))continue;
            BufferedImage input=ImageIO.read(file);
            int w=input.getWidth(),h=input.getHeight();
            int[] rgba=input.getRGB(0,0,w,h,null,0,w);
            RestaurantVision.Decision d=engine.inspect(rgba,w,h);
            if(d.kind!=RestaurantVision.Kind.WAIT) {
                if(d.x<0||d.x>=w||d.y<0||d.y>=h)
                    throw new AssertionError("Outside screenshot: "+file.getName()+" "+d);
            }
            if(file.getName().equals("user_stuck_tutorial.png")) {
                if(d.kind!=RestaurantVision.Kind.TUTORIAL_TAP ||
                   d.x<125 || d.x>175 || d.y<300 || d.y>360)
                    throw new AssertionError("Must tap user's exact tutorial spotlight at ~150,335: "+d);
            }
            if(file.getName().equals("video01_08.jpg") || file.getName().equals("video01_54.jpg")){
                if(d.kind!=RestaurantVision.Kind.TUTORIAL_TAP)
                    throw new AssertionError("Missed tutorial from recording: "+file.getName()+" "+d);
            }
            if(file.getName().equals("video01_10.jpg") || file.getName().equals("video01_18.jpg")){
                if(d.kind==RestaurantVision.Kind.TUTORIAL_TAP)
                    throw new AssertionError("False tutorial over real upgrade panel: "+d);
            }
            if(file.getName().startsWith("video") &&
               !file.getName().startsWith("video01_") &&
               d.kind==RestaurantVision.Kind.TUTORIAL_TAP)
                throw new AssertionError("False tutorial on unrelated game screen: "+file.getName());
            // Known safe actions from actual supplied videos:
            if(file.getName().equals("video04.jpg") || file.getName().equals("video11.jpg") ||
               file.getName().equals("video03.jpg") || file.getName().equals("manage_raw720.jpg") ||
               file.getName().equals("level_raw720.jpg")) {
                if(d.kind!=RestaurantVision.Kind.BUY_WITH_COINS)
                    throw new AssertionError("Coin button missed: "+file.getName()+" "+d);
            }
            if(file.getName().equals("video09.jpg") && d.kind!=RestaurantVision.Kind.CLOSE_PANEL)
                throw new AssertionError("Gem/refund popup must be dismissed, not purchased: "+d);
            if(file.getName().equals("video05.jpg") && d.kind==RestaurantVision.Kind.CLOSE_PANEL)
                throw new AssertionError("False-positive panel on running game");
            System.out.println(file.getName()+" -> "+d);
            if(d.kind==RestaurantVision.Kind.BUY_WITH_COINS)coin++;
            if(d.kind==RestaurantVision.Kind.TUTORIAL_TAP)tutorial++;
            count++;
        }
        if(count<11)throw new AssertionError("Expected examples across 11 videos");
        if(coin<2)throw new AssertionError("Failed to recognize known coin-upgrade screens");
        // Regression: muted green gem-only top dialog must not be a coin purchase.
        if(tutorial<3)throw new AssertionError("Missing tutorial coverage");
        System.out.println("PASS: "+count+" screenshots including 3 tutorial targets, "+coin+" coin detections");
    }
}
