import Devices.Heater;
import Devices.SHManager;
import Devices.Thermometer;
import Devices.AirConditioner;
import Devices.Light;
import Devices.AudioDevice;
import Network.UserDevice;
import Devices.CoffeeMachine;

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

        // Swiatlo
        Light lampa_salon = new Light();
        menadzer.registerDevice("lampa_salon", lampa_salon);
        Light ledy_kuchnia = new Light();
        menadzer.registerDevice("ledy_kuchnia", ledy_kuchnia);
        Light swiatla_sypialnia = new Light();
        menadzer.registerDevice("swiatla_sypialnia", swiatla_sypialnia);
        Light swiatla_toaleta = new Light();
        menadzer.registerDevice("swiatla_toaleta", swiatla_toaleta);


        //Klimatyzacja
        AirConditioner klima = new AirConditioner();
        menadzer.registerDevice("klimatyzacja", klima);

        //(< 15 stopni)
        menadzer.registerLogic(
                grzejnik,
                "SET_POWER",
                new String[] {"10"},
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.LESS_THAN, "15")
        );

        //Wyłączamy klimatyzację
        menadzer.registerLogic(
                klima,
                "SET_COOLING",
                new String[] {"0"},
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.LESS_THAN, "15")
        );

        //(> 24 stopnie)
        //Wyłączamy grzejnik
        menadzer.registerLogic(
                grzejnik,
                "SET_POWER",
                new String[] {"0"},
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.GREATER_THAN, "24")
        );
        //Włączamy klimatyzację
        menadzer.registerLogic(
                klima,
                "SET_COOLING",
                new String[] {"10"},
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.GREATER_THAN, "24")
        );

        //Audio
        AudioDevice soundbarSalon = new AudioDevice();
        AudioDevice glosnikSypialnia = new AudioDevice();
        AudioDevice glosnikKuchnia = new AudioDevice();

        menadzer.registerDevice("audio_salon", soundbarSalon);
        menadzer.registerDevice("audio_sypialnia", glosnikSypialnia);
        menadzer.registerDevice("audio_kuchnia", glosnikKuchnia);

        //COFFEE
        CoffeeMachine ekspres = new CoffeeMachine();
        menadzer.registerDevice("ekspres_kuchnia", ekspres);


        // Jeśli temperatura spadnie poniżej 10 stopni, zrób kawę (na rozgrzanie).
        menadzer.registerLogic(
                ekspres,
                "MAKE_COFFEE",
                new String[] {},
                menadzer.createComparator(wewnetrzny, "GET_TEMPERATURE", SHManager.Comparator.Condition.LESS_THAN, "10")
        );

        // Opcjonalnie: Ustawienie timera na start
        ekspres._setTimer(50); // Za 50 cykli ekspres sam ruszy



        UI ui = new UI(zewnetrzny, wewnetrzny, grzejnik);
    }
}
