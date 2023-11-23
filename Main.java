import java.util.Scanner;

public class Main {
    public static void main(String argv[]) {

        System.out.println("Entering the digital market place...");
        System.out.println("Please enter the appropriate login:");
        System.out.println("1 - Seller");
        System.out.println("2 - Buyer");

        try (Scanner scan = new Scanner(System.in)) {
            int user_input = scan.nextInt();

            if(user_input == 1) {
                System.out.println("\nLogged in as Seller.\t\n");

                new Thread(new Seller()).start();

            } // End if
            else if(user_input == 2) {
                System.out.println("\nLogged in as a Buyer.\t\n");

                new Thread(new Buyer()).start();

            }// End else if
            else {
                System.out.println("Unkown login input, please try again:");   
                
            } // End else
        }// End try

    }

}