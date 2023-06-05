import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.*;
import java.util.ArrayList;

public class Servidor {
    public static void main(String[] args) {
        MarcoServidor miMarco = new MarcoServidor();
        miMarco.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MarcoServidor extends JFrame implements Runnable {
    private JTextArea areaTexto;

    public MarcoServidor() {
        setBounds(1200, 300, 280, 350);
        JPanel miLamina = new JPanel();
        miLamina.setLayout(new BorderLayout());
        areaTexto = new JTextArea();
        miLamina.add(areaTexto, BorderLayout.CENTER);
        add(miLamina);
        setVisible(true);
        Thread miHilo = new Thread(this);
        miHilo.start();
    }

    @Override
    public void run() {
        try {
            ServerSocket servidor = new ServerSocket(3690);
            String nick, ip, mensaje;
            EnvioDePaquete paqueteRecibido;
            ArrayList<String> listaIp = new ArrayList<>();

            while (true) {
                Socket miSocket = servidor.accept();
                ObjectInputStream paquete = new ObjectInputStream(miSocket.getInputStream());
                paqueteRecibido = (EnvioDePaquete) paquete.readObject();
                nick = paqueteRecibido.getNick();
                ip = paqueteRecibido.getIp();
                mensaje = paqueteRecibido.getMensaje();

                // Verifica si el mensaje es un archivo
                if (mensaje.startsWith("Archivo:")) {
                    String nombreArchivo = mensaje.substring(8);
                    recibirArchivo(nombreArchivo, miSocket);

                    // Reenviar archivo a la IP de destino
                    Socket enviaDestinatario = new Socket(ip, 9630);
                    enviarArchivo(nombreArchivo, enviaDestinatario);
                    enviaDestinatario.close();
                } else if (!mensaje.equals(" Online")) {
                    areaTexto.append("\n" + nick + ":" + mensaje + " para " + ip);
                    Socket enviaDestinatario = new Socket(ip, 9630);
                    ObjectOutputStream reenvioPaquete = new ObjectOutputStream(enviaDestinatario.getOutputStream());
                    reenvioPaquete.writeObject(paqueteRecibido);
                    enviaDestinatario.close();
                    reenvioPaquete.close();
                    miSocket.close();
                } else {
                    InetAddress localizacion = miSocket.getInetAddress();
                    String ipRemota = localizacion.getHostAddress();
                    paqueteRecibido.setListaIp(listaIp);
                    listaIp.add(ipRemota);
                    for (String z : listaIp) {
                        System.out.println("Array: " + z);
                        Socket enviaDestinatario = new Socket(z, 9630);
                        ObjectOutputStream reenvioPaquete = new ObjectOutputStream(enviaDestinatario.getOutputStream());
                        reenvioPaquete.writeObject(paqueteRecibido);
                        enviaDestinatario.close();
                        reenvioPaquete.close();
                        miSocket.close();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private void recibirArchivo(String nombreArchivo, Socket socket) {
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            int tamanoArchivo = dataInputStream.readInt();
            areaTexto.append("\n" + "Recibiendo archivo: " + nombreArchivo + " (" + tamanoArchivo + " bytes)");

            FileOutputStream fileOutputStream = new FileOutputStream(nombreArchivo);
            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalBytesRead = 0;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (totalBytesRead >= tamanoArchivo) {
                    break;
                }
            }
            areaTexto.append("\n" + "Archivo recibido: " + nombreArchivo);
            fileOutputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarArchivo(String nombreArchivo, Socket socket) {
        try {
            File archivo = new File(nombreArchivo);
            FileInputStream fileInputStream = new FileInputStream(archivo);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeInt((int) archivo.length());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
            dataOutputStream.flush();
            fileInputStream.close();
            areaTexto.append("\n" + "Archivo enviado: " + nombreArchivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
