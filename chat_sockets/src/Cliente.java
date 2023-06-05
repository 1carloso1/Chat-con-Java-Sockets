import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Cliente {
    public static void main(String[] args) {
        MarcoCliente miMarco = new MarcoCliente();
        miMarco.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class MarcoCliente extends JFrame {
    public MarcoCliente() {
        setBounds(600, 300, 280, 350);
        LaminaMarcoCliente miLamina = new LaminaMarcoCliente();
        add(miLamina);
        setVisible(true);
        addWindowListener(new EnvioOnline()); // se ejecuta el envioOnline
    }
}

//-----Envio señal Online-----
class EnvioOnline extends WindowAdapter {
    public void windowOpened(WindowEvent e) { //Cuando se abra la ventana se enviara la señal de que el usuario esta en linea
        try {
            Socket miSocket = new Socket("192.168.1.64", 3690); //la ip del servidor
            EnvioDePaquete paqueteEnviado = new EnvioDePaquete();
            paqueteEnviado.setMensaje(" Online");
            ObjectOutputStream paqueteDatos = new ObjectOutputStream(miSocket.getOutputStream());
            paqueteDatos.writeObject(paqueteEnviado);
            miSocket.close();
        } catch (Exception ee) {
            System.out.println(ee.getMessage()); //manda como mensaje el error
        }
    }
}
//-----Final Envio señal Online-----

class LaminaMarcoCliente extends JPanel implements Runnable {//Runnable nos servira para que el servidor este permanentemente a la escucha
    private JTextField mensaje;
    private JComboBox<String> ip;
    private JLabel nick;
    private JButton botonEnviar;
    private JButton botonEnviarArchivo;
    private JTextArea campoChat;

    public LaminaMarcoCliente() {
        String nickUsuario = JOptionPane.showInputDialog("Nick: ");
        JLabel n_nick = new JLabel("Nick: ");
        add(n_nick);
        nick = new JLabel();
        nick.setText(nickUsuario);
        add(nick);
        JLabel texto = new JLabel("Online: ");
        add(texto);
        ip = new JComboBox<>();
        add(ip);
        campoChat = new JTextArea(12, 20);
        add(new JScrollPane(campoChat));
        mensaje = new JTextField(20);
        add(mensaje);
        botonEnviar = new JButton("Enviar");
        EnviaTexto eventoMensaje = new EnviaTexto();
        botonEnviar.addActionListener(eventoMensaje);
        add(botonEnviar);
        botonEnviarArchivo = new JButton("Archivo");
        EnviaArchivo eventoArchivo = new EnviaArchivo();
        botonEnviarArchivo.addActionListener(eventoArchivo);
        add(botonEnviarArchivo);
        Thread miHilo = new Thread(this);//Creamos un hilo
        miHilo.start();
    }

    @Override
    public void run() {
        try {
            ServerSocket servidorCliente = new ServerSocket(9630);
            Socket cliente;
            EnvioDePaquete paqueteRecibido;
            while (true) {
                cliente = servidorCliente.accept();
                ObjectInputStream paquete = new ObjectInputStream(cliente.getInputStream());
                paqueteRecibido = (EnvioDePaquete) paquete.readObject();

                if (!paqueteRecibido.getMensaje().startsWith("Archivo:")) {
                    if (!paqueteRecibido.getMensaje().equals(" Online")) {
                        campoChat.append("\n" + paqueteRecibido.getNick() + ":" + paqueteRecibido.getMensaje());
                    } else {
                        ArrayList<String> ipMenu;
                        ipMenu = paqueteRecibido.getListaIp();
                        ip.removeAllItems();
                        for (String z : ipMenu) {
                            ip.addItem(z);
                        }
                    }
                } else {
                    String nombreArchivo = paqueteRecibido.getMensaje().substring(8);
                    recibirArchivo(nombreArchivo, cliente);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private class EnviaTexto implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            campoChat.append("\n" + mensaje.getText());//Se acumula lo que escribimos en el chat
            try {
                Socket miSocket = new Socket("192.168.1.64", 3690); //Los numero magicos segun Nicola Tesla jiji
                EnvioDePaquete paqueteEnviado = new EnvioDePaquete();
                paqueteEnviado.setNick(nick.getText());
                paqueteEnviado.setIp(ip.getSelectedItem().toString());  //se almacena toda la informacion en el objeto "datos"
                paqueteEnviado.setMensaje(mensaje.getText());
                ObjectOutputStream paquete = new ObjectOutputStream(miSocket.getOutputStream()); //Se crea un flujo de datos de salida
                paquete.writeObject(paqueteEnviado); //se almacena en el flujo el paquete que se enviara
                miSocket.close(); //se cierra el flujo de salida
            } catch (IOException ex) {
                System.out.println(ex.getMessage()); //manda como mensaje el error
            }
        }
    }

    private class EnviaArchivo implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            int seleccion = fileChooser.showOpenDialog(LaminaMarcoCliente.this);
            if (seleccion == JFileChooser.APPROVE_OPTION) {
                File archivoSeleccionado = fileChooser.getSelectedFile();
                String rutaArchivo = archivoSeleccionado.getAbsolutePath();

                try {
                    Socket socket = new Socket("192.168.1.64", 3690);
                    EnvioDePaquete paqueteEnviado = new EnvioDePaquete();
                    paqueteEnviado.setNick(nick.getText());
                    paqueteEnviado.setIp(ip.getSelectedItem().toString());
                    paqueteEnviado.setMensaje("Archivo:" + archivoSeleccionado.getName());
                    ObjectOutputStream paqueteDatos = new ObjectOutputStream(socket.getOutputStream());
                    paqueteDatos.writeObject(paqueteEnviado);

                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    FileInputStream fileInputStream = new FileInputStream(archivoSeleccionado);

                    // Envía el tamaño del archivo al servidor
                    dataOutputStream.writeInt((int) archivoSeleccionado.length());

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    int totalBytesRead = 0;
                    double tiempoInicio = System.currentTimeMillis();
                    double tiempoTranscurrido = 0;
                    double tasaTransferencia = 0;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        dataOutputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        tiempoTranscurrido = (System.currentTimeMillis() - tiempoInicio) / 1000.0;
                        tasaTransferencia = totalBytesRead / tiempoTranscurrido;
                        String tiempoTranscurridoFormateado = formatearTiempo(tiempoTranscurrido);
                        String tasaTransferenciaFormateada = formatearTasaTransferencia(tasaTransferencia);
                        System.out.println("Tasa de transferencia: " + tasaTransferenciaFormateada + "/s" + " - Tiempo transcurrido: " + tiempoTranscurridoFormateado);
                    }

                    campoChat.append("\n" + "Archivo enviado: " + archivoSeleccionado.getName());//Se acumula lo que escribimos en el chat

                    fileInputStream.close();
                    dataOutputStream.close();
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private String formatearTiempo(double tiempo) {
            int minutos = (int) (tiempo / 60);
            int segundos = (int) (tiempo % 60);
            return String.format("%02d:%02d", minutos, segundos);
        }

        private String formatearTasaTransferencia(double tasa) {
            String[] unidades = {"B/s", "KB/s", "MB/s", "GB/s", "TB/s"};
            int index = 0;
            while (tasa >= 1024 && index < unidades.length - 1) {
                tasa /= 1024;
                index++;
            }
            return String.format("%.2f %s", tasa, unidades[index]);
        }
    }

    private void recibirArchivo(String nombreArchivo, Socket socket) {
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            int tamanoArchivo = dataInputStream.readInt();
            System.out.println("Recibiendo archivo: " + nombreArchivo + " (" + tamanoArchivo + " bytes)");

            FileOutputStream fileOutputStream = new FileOutputStream(nombreArchivo);
            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalBytesRead = 0;
            double tiempoInicio = System.currentTimeMillis();
            double tiempoTranscurrido = 0;
            double tasaTransferencia = 0;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                tiempoTranscurrido = (System.currentTimeMillis() - tiempoInicio) / 1000.0;
                tasaTransferencia = totalBytesRead / tiempoTranscurrido;
                String tiempoTranscurridoFormateado = formatearTiempo(tiempoTranscurrido);
                String tasaTransferenciaFormateada = formatearTasaTransferencia(tasaTransferencia);
                System.out.println("Tasa de transferencia: " + tasaTransferenciaFormateada + "/s" + " - Tiempo transcurrido: " + tiempoTranscurridoFormateado);
            }
            System.out.println("Archivo recibido: " + nombreArchivo);
            fileOutputStream.close();
            socket.close();

            // Agrega el mensaje de archivo recibido al campo de texto
            campoChat.append("\n" + "Archivo recibido: " + nombreArchivo + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatearTiempo(double tiempo) {
        int minutos = (int) (tiempo / 60);
        int segundos = (int) (tiempo % 60);
        return String.format("%02d:%02d", minutos, segundos);
    }

    private String formatearTasaTransferencia(double tasa) {
        String[] unidades = {"B/s", "KB/s", "MB/s", "GB/s", "TB/s"};
        int index = 0;
        while (tasa >= 1024 && index < unidades.length - 1) {
            tasa /= 1024;
            index++;
        }
        return String.format("%.2f %s", tasa, unidades[index]);
    }

}

class EnvioDePaquete implements Serializable {
    private String nick;
    private String ip;
    private String mensaje;
    private ArrayList<String> listaIp;

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setListaIp(ArrayList<String> listaIp) {
        this.listaIp = listaIp;
    }

    public String getNick() {
        return nick;
    }

    public String getIp() {
        return ip;
    }

    public String getMensaje() {
        return mensaje;
    }

    public ArrayList<String> getListaIp() {
        return listaIp;
    }
}
