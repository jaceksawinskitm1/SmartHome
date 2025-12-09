import Devices.Heater;
import Devices.SHManager;
import Devices.Thermometer;
import Network.UserDevice;

public class Main {
    public static void main(String[] args) {
        SHManager menadzer = new SHManager();

        Heater grzejnik = new Heater();

        Thermometer zewnetrzny = new Thermometer();

        Thermometer wewnetrzny = new Thermometer();
        wewnetrzny._enableTempSimulation(zewnetrzny);


        zewnetrzny._changeTemp(-5);


        menadzer.registerDevice("grzejnik", grzejnik);
        menadzer.registerDevice("zewnetrzny", zewnetrzny);
        menadzer.registerDevice("wewnetrzny", wewnetrzny);


        // Wyłącz grzejnij jak temperatura będzie powyżej 15 stopni
        menadzer.registerLogic(
                grzejnik,
                "SET_POWER",
                new String[] {"0"},
                // Conditions
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.GREATER_EQUAL, "15")
        );

        // Ustaw grzejnik na 5 jeżeli temperatura jest pomiędzy 10 a 15 stopni
        menadzer.registerLogic(
                grzejnik,
                "SET_POWER",
                new String[] {"5"},
                // Conditions
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.LESS_THAN, "15"),
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.GREATER_EQUAL, "10")
        );

        // Ustaw grzejnik na 10 jeżeli temperatura jest poniżej 10 stopni
        menadzer.registerLogic(
                grzejnik,
                "SET_POWER",
                new String[] {"10"},
                // Conditions
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.LESS_THAN, "10")
        );


        grzejnik.addEvent(() -> {
            System.out.println("Moc grzejnika: " + grzejnik.getPower());
        });


        UserDevice user = new UserDevice(true, menadzer.getNetworkManager(), menadzer);
        //TODO: Request priority

        UI ui = new UI(zewnetrzny, wewnetrzny, grzejnik);
    }
}
