import com.example.froggypilot.RestaurantVision;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** JVM-only smoke tests: all training snapshots can be parsed without crashes. */
public final class VisionTest {
    public static void main(String[] args) throws Exception {
        RestaurantVision engine=new RestaurantVision(new RestaurantVision.Options());
        File folder=new File("tests/frames");
        int count=0,coin=0;
        for(File file:folder.listFiles()) {
            if(!file.getName().endsWith(".jpg"))continue;
            BufferedImage input=ImageIO.read(file);
            int w=input.getWidth(),h=input.getHeight();
            int[] rgba=input.getRGB(0,0,w,h,null,0,w);
            RestaurantVision.Decision d=engine.inspect(rgba,w,h);
            if(d.kind!=RestaurantVision.Kind.WAIT) {
                if(d.x<0||d.x>=w||d.y<0||d.y>=h)
                    throw new AssertionError("Outside screenshot: "+file.getName()+" "+d);
            }
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
            count++;
        }
        if(count<11)throw new AssertionError("Expected examples across 11 videos");
        if(coin<2)throw new AssertionError("Failed to recognize known coin-upgrade screens");
        // Regression: muted green gem-only top dialog must not be a coin purchase.
        System.out.println("PASS: "+count+" screenshot examples, "+coin+" coin purchase detections");
    }
}
