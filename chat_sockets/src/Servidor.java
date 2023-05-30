import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.*;

public class Servidor  {
    public static void main(String[] args) {
        MarcoServidor miMarco=new MarcoServidor();
        miMarco.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MarcoServidor extends JFrame implements Runnable{//Runnable nos servira para que el servidor este permanentemente a la escucha

    private	JTextArea areaTexto;

    public MarcoServidor(){
        setBounds(1200,300,280,350);
        JPanel miLamina= new JPanel();
        miLamina.setLayout(new BorderLayout());
        areaTexto =new JTextArea();
        miLamina.add(areaTexto,BorderLayout.CENTER);
        add(miLamina);
        setVisible(true);
        Thread miHilo = new Thread(this); //Creamos un hilo
        miHilo.start();
    }

    @Override
    public void run() { // el codigo que se encargara de la escucha se encontrara aqui
        try {
            ServerSocket servidor = new ServerSocket(3690);//Con esto esta a la escucha y que abra este puerto
            String nick, ip, mensaje;
            EnvioDePaquete paqueteRecibido;

            while(true) { //Creamos un bucle infinito para que fluya el chat
                Socket miSocket = servidor.accept(); //Con esto acepta las conexiones del exterior con este puerto
                ObjectInputStream paquete = new ObjectInputStream(miSocket.getInputStream());//Se crea un flujo de datos de entrada
                paqueteRecibido = (EnvioDePaquete)paquete.readObject(); //se almacena el paquete recibido
                nick = paqueteRecibido.getNick();
                ip = paqueteRecibido.getIp();//se almacena toda la informacion del paquete recibido
                mensaje = paqueteRecibido.getMensaje();
                areaTexto.append("\n" + nick + ":" + mensaje + " para " + ip);
                Socket enviaDestinatario = new Socket(ip,9630); // se reenviara el paquete recibido al servidor a la ip destino
                ObjectOutputStream reenvioPaquete = new ObjectOutputStream(enviaDestinatario.getOutputStream()); //Se crea un flujo de datos de salida
                reenvioPaquete.writeObject(paqueteRecibido); //se almacena en el flujo el paquete que se reenviara
                enviaDestinatario.close();//se cierra el socket
                miSocket.close();//se cierra la conexión
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage()); //manda como mensaje el error
        }
    }
}
