package edu.cmu.sei.ttg.aaiot.rs;

/**
 * Created by Sebastian on 2017-05-02.
 */
public class main {
   public static void main(String[] args)
    {
        try {
            Controller controller = new Controller();
            controller.run();

        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

}
