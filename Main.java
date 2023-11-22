import java.util.Scanner;

public class Main {
    public static void main(String argv[]) {

        System.out.println("Choose Which Thread to Start:");
        System.out.println("1 - Seller");
        System.out.println("2 - Buyer");

        try (Scanner scan = new Scanner(System.in)) {
            int user_input = scan.nextInt();

            if(user_input == 1) {
                System.out.println("\nYou have chosen: Seller.\t\n");

                new Thread(new Seller()).start();

            } // End if
            else if(user_input == 2) {
                System.out.println("\nYou have chosen: Buyer.\t\n");

                new Thread(new Buyer()).start();

            }// End else if
            else {
                System.out.println("Unknown Input. Please Try again!");   
                
            } // End else
        }// End try

    }

}