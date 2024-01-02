import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable {

    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;
    private static char PRIVATE_SIGN = '@';

    public final static ArrayList<ClientManager> clients = new ArrayList<>();

    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к чату. (для сервера)"); // Сообщение летит на сервер
            broadcastMessage("Server",  name + " подключился к чату. (для пользователей)");
        }
        catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }


    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                if(isPrivateMessage(messageFromClient)){
                    sendPrivateMessage(messageFromClient);
                }else{
                    broadcastMessage(name, messageFromClient);
                }

            }
            catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    /*
    Проверка на наличие приватного знака
     */
    private boolean isPrivateMessage(String messageFromClient) {
        return messageFromClient.trim().charAt(0) == PRIVATE_SIGN;
    }

    /*
    Отправка личного сообщения
     */
    private void sendPrivateMessage(String message){
        // кому отсылаем
        String addressName = getAddressName(message);

        if(addressName != null){
            // от кого
            String fromAddress = name;
            // сообщение без приватного префикса
            String privateMessage = getMessage(message);
            // флаг о доставке сообщение
            boolean isDelivered = false;

            for (ClientManager client: clients) {
                try {
                    // если нашли адресат в чате, то отправляем сообщение
                    if (client.name.toLowerCase().equals(addressName)) {
                        client.bufferedWriter.write(String.format("private message from %s: %s", fromAddress, privateMessage));
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                        // меняем статус отправки
                        isDelivered = !isDelivered;
                        break;
                    }
                }
                catch (IOException e){
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
            // если адресата нет в чате, то выводим сообщение только пользователю
            if(!isDelivered)
                serverAnswer((String.format("Пользователя %s нет в чате", addressName)));
        }
    }

    /*
    для отправки уведомлений пользователю
     */
    private void serverAnswer(String answer) {
        for (ClientManager client: clients) {
            try {
                // находим себя
                if (client.name.equals(name)) {
                    client.bufferedWriter.write(answer);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                    break;
                }
            }
            catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    /*
    Вырезаем имя адресата из сообщения
     */
    private String getMessage(String message) {
        return  message.trim().substring(message.indexOf(" ")).trim();
    }

    /*
    Получаем имя пользователя для приватного сообщения,
    либо null если нету имени или нету сообщения
     */
    private String getAddressName(String message) {
        int index = message.trim().indexOf(" ");
        if(index > 1){
            return message.trim().substring(1, index).toLowerCase();
        }
        return null;
    }

    private void broadcastMessage(String from, String message){
        for (ClientManager client: clients) {
            try {
                if (!client.name.equals(from)) {
                    client.bufferedWriter.write(from + ": " + message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                }
            }
            catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeClient(){
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server", name + " покинул чат.");
    }

}
