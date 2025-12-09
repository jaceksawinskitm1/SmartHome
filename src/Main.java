import Devices.Heater;
import Devices.SHManager;
import Devices.Thermometer;

public class Main {
    public static void main(String[] args) {
        SHManager menadzer = new SHManager();

        Heater grzejnik = new Heater();

        Thermometer zewnetrzny = new Thermometer();

        Thermometer wewnetrzny = new Thermometer();
        wewnetrzny._enableTempSimulation(zewnetrzny);


        zewnetrzny._changeTemp(-5);

        wewnetrzny.addEvent(() -> {
            if (wewnetrzny.getTemperature() < 10) {
                grzejnik.setPower(10);
            }
            else if (wewnetrzny.getTemperature() < 15) {
                grzejnik.setPower(5);
            }
            else if (wewnetrzny.getTemperature() >= 15) {
                grzejnik.setPower(0);
            }
        });


        grzejnik.addEvent(() -> {
            System.out.println("Moc grzejnika: " + grzejnik.getPower());
        });



        menadzer.addDevice(grzejnik);
        menadzer.addDevice(zewnetrzny);
        menadzer.addDevice(wewnetrzny);


        UI ui = new UI(zewnetrzny, wewnetrzny, grzejnik);
    }
}
