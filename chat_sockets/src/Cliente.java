import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        MarcoCliente miMarco=new MarcoCliente();
        miMarco.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MarcoCliente extends JFrame{
    public MarcoCliente(){
        setBounds(600,300,280,350);
        LaminaMarcoCliente miLamina=new LaminaMarcoCliente();
        add(miLamina);
        setVisible(true);
    }
}

class LaminaMarcoCliente extends JPanel implements Runnable{//Runnable nos servira para que el servidor este permanentemente a la escucha
    private JTextField mensaje, nick, ip;
    private JButton miBoton;
    private JTextArea campoChat;

    public LaminaMarcoCliente(){
        nick = new JTextField(5);
        add(nick);
        JLabel texto = new JLabel("-CHAT-");
        add(texto);
        ip = new JTextField(8);
        add(ip);
        campoChat = new JTextArea(12,20);
        add(campoChat);
        mensaje =new JTextField(20);
        add(mensaje);
        miBoton =new JButton("Enviar");
        EnviaTexto miEvento = new EnviaTexto();
        miBoton.addActionListener(miEvento);
        add(miBoton);
        Thread miHilo = new Thread(this);//Creamos un hilo
        miHilo.start();
    }

    @Override
    public void run() {// el codigo que se encargara de la escucha se encontrara aqui
        try {
            ServerSocket servidorCliente = new ServerSocket(9630); //Con esto esta a la escucha y que abra este puerto
            Socket cliente;
            EnvioDePaquete paqueteRecibido;
            while (true){ //ciclo infinito para que este permanentemente a la escucha
                cliente = servidorCliente.accept(); //de esta forma el socket acepta todas las conexiones que vengan del exterior
                ObjectInputStream paquete = new ObjectInputStream(cliente.getInputStream()); //Se crea un flujo de datos de entrada
                paqueteRecibido = (EnvioDePaquete) paquete.readObject();
                campoChat.append("\n" + paqueteRecibido.getNick() + ":" + paqueteRecibido.getMensaje() + " para " + paqueteRecibido.getIp());
            }
        } catch(Exception e){
            System.out.println(e.getMessage()); //manda como mensaje el error
        }
    }

    private class EnviaTexto implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Socket miSocket = new Socket("192.168.1.64",3690); //Los numero magicos segun Nicola Tesla jiji
                EnvioDePaquete paqueteEnviado = new EnvioDePaquete();
                paqueteEnviado.setNick(nick.getText());
                paqueteEnviado.setIp(ip.getText());  //se almacena toda la informacion en el objeto "datos"
                paqueteEnviado.setMensaje(mensaje.getText());
                ObjectOutputStream paquete = new ObjectOutputStream(miSocket.getOutputStream()); //Se crea un flujo de datos de salida
                paquete.writeObject(paqueteEnviado); //se almacena en el flujo el paquete que se enviara
                miSocket.close(); //se cierra el flujo de salida

            } catch (IOException ex) {
                System.out.println(ex.getMessage()); //manda como mensaje el error
            }
        }
    }
}

class EnvioDePaquete implements Serializable { //Con esto indicamos que todas las instancias son capaces de convertirse en bytes
    private String nick, ip, mensaje;

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }
}