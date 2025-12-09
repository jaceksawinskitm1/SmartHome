import Devices.Heater;
import Devices.Thermometer;

import javax.swing.*;
import java.awt.*;
/*
    Tutaj przydaloby sie dac jakis slider to temperatury na zewnatrz - zmienic temperature mozna przez zewnatrz.setTemperature(...)
    Jak bedziesz dawal labele z jakimikolwiek informacjami ktore sie bedą zmienialy,
    masz do tego funkcje addEvent(...) w kazdym z urzadzeń. Uzywasz jej tak:

    urzadzenie.addEvent(() -> {
        (to co tutaj jest, bedzie wykonane co sekunde w tym urzadzeniu, wiec mozna dac cos w tym stylu)
        label.setText(urzadenie.getJakasWartosc())
    });

    Grzejnik ma metody getPower() i setPower(...),
    ale ta druga nie powinna byc potrzebna bo Main juz obsługuje logike grzejnika, wiec powinien sie sam ustawiac na dobra moc.

    Dodatkowo mozna by bylo dac jakis oddzielny ui jako taki "finalny" ui aplikacji,
    w stylu ze nie ma tam slidera od temperatury itp, tylko wszystkie rzeczy w stylu dodaj urzadenie, ale to mozna pozniej zrobic.

 */


public class UI extends JFrame {
    public UI(Thermometer zewnatrz, Thermometer wewnatrz, Heater grzejnik) {
        this.setSize(400, 400);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel zew_lab = new JLabel("Temperatura na zewnątrz:");
        JSlider zew_tem = new JSlider(SwingConstants.VERTICAL, -40, 40, 10);
        zew_tem.addChangeListener((event) -> {zewnatrz._changeTemp(zew_tem.getValue());});
        zewnatrz.addEvent(() -> {zew_lab.setText(zewnatrz.getTemperature() + " stopni");});

        JLabel wew_lab = new JLabel("Temperatura wewnątrz:");
        JLabel wew_tem = new JLabel();
        zewnatrz.addEvent(() -> {wew_tem.setText(wewnatrz.getTemperature() + " stopni");});

        JLabel grz_lab = new JLabel("Moc grzejnika:");
        JLabel grz_moc = new JLabel();
        zewnatrz.addEvent(() -> {grz_moc.setText("" + grzejnik.getPower());});

        this.setLayout(new GridLayout(3, 2));

        this.add(zew_lab);
        this.add(zew_tem);

        this.add(wew_lab);
        this.add(wew_tem);

        this.add(grz_lab);
        this.add(grz_moc);

        this.setVisible(true);
    }
}
