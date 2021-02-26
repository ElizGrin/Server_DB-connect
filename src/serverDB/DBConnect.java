package serverDB;

import java.awt.Color;
import java.net.*;
import java.sql.*;
import java.io.*;
import javax.swing.*;
import javax.swing.table.*;

public class DBConnect extends JFrame {
    ServerSocket sock;

    String st;
    String stroka;
    JTable table1;
    JScrollPane sp;
    static String uNumber, author, bookName, publisher, year;

    Statement sq;
    DefaultTableModel DTM;
    Connection db;
    String colheads[] = {"ISBN-number", "Author", "Name", "Publisher", "Year"};
    static Object dataConditer[][];
    String readmessage;
    String newNum, newAuth, newName, newPubl, newYear;

    static class ClientThread extends Thread{
        Socket socket;
        ClientThread(Socket sock){
            this.socket = sock;
        }
        public void run(){
            try{
                BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter os = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                new DBConnect("Server", is, os);
            } catch (IOException | SQLException | ClassNotFoundException e) {
            }
        }

    }


    public DBConnect(String Title, BufferedReader in, BufferedWriter out) throws SQLException, IOException, ClassNotFoundException {
        super(Title);
        setLayout(null);
        DTM = new DefaultTableModel(dataConditer, colheads);
        table1 = new JTable(DTM);
        table1.setBackground(Color.getHSBColor(159, 216, 234));
        JScrollPane sp = new JScrollPane(table1, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBounds(100, 5, 600, 600);
        add(sp);
        this.setSize(800, 700);
        this.setVisible(true);
            Class.forName("com.mysql.cj.jdbc.Driver");//получение класса драйвера
            db = DriverManager.getConnection("jdbc:mysql://localhost/psp?useUnicode=true&serverTimezone=UTC&useSSL=false", "root",
                    "existentialcrisis");
            sq = db.createStatement();  //создание экзепмпляра класса для выполнения запросов
            StringBuffer x = new StringBuffer();
            String sq_str = "SELECT * FROM BOOKS";
            ResultSet rs = sq.executeQuery(sq_str); //возвращает экземпляр класса ResultSet (для получения множества объектов, удовл условию)
            while (rs.next()) {

                uNumber = rs.getString("ISBN");
                author = rs.getString("Author");
                bookName = rs.getString("Name");
                publisher = rs.getString("Publisher");
                year = rs.getString("PublishYear");


                Object addingData[] = {uNumber, author, bookName, publisher, year};
                DTM.addRow(addingData);
                st = (uNumber + "/" + author + "/" + bookName + "/" + publisher + "/" + year + "/");
                DTM.fireTableDataChanged();

                x.append(st);
            }

            while (true) {
                boolean flag = true;
                String str, res;
                while (flag) {
                    readmessage = in.readLine().trim();
                    if (readmessage.compareTo("Disconnect") == 0) {
                        flag = false;
                        break;
                    } else {
                        String searchByName = null;
                        String arrStr[] = readmessage.split("/");
                        if (arrStr.length == 2) {
                            res = arrStr[0];
                            str = arrStr[1];
                            if (res.compareTo("searchpartly") == 0) {
                                searchByName = "SELECT * FROM BOOKS WHERE Name = '%" + str + "%'";
                            }
                            if (res.compareTo("searchfull") == 0) {
                                searchByName = "SELECT * FROM BOOKS WHERE Name = '" + str + "'";
                            }

                            rs = sq.executeQuery(searchByName);
                            while (rs.next()) {

                                uNumber = rs.getString("ISBN");
                                author = rs.getString("Author");
                                bookName = rs.getString("Name");
                                publisher = rs.getString("Publisher");
                                year = rs.getString("PublishYear");

                                if (str.compareTo(bookName) == 0) {
                                    out.write("found full\n");
                                    out.flush();
                                } else {
                                    out.write("partly\n");
                                    out.flush();
                                }

                                st = ("ISBN: " + uNumber + "; Author: " + author + "; Bookname: " +
                                        bookName + "; Publisher: " + publisher + "; Year: " + year + "\n");
                                System.out.println(st);
                            }
                            out.write("end or not found\n");
                            out.flush();
                        }
                        if (arrStr.length == 1) {   //delete-search-Operation
                            String number = arrStr[0];
                            String sql_delete = "DELETE FROM BOOKS WHERE ISBN ='" + number + "' ";
                            int deletedRows = sq.executeUpdate(sql_delete); // не возвращает данные из таблицы
                            if (deletedRows != 0) {
                                for (int i = 0; i < DTM.getRowCount(); i++) {
                                    if (DTM.getValueAt(i, 0).equals(number)) {
                                        DTM.removeRow(i);
                                        DTM.fireTableDataChanged();
                                    }
                                }
                                out.write("The String is deleted\n");
                                out.flush();
                            } else System.out.println("Error occurred");
                        }
                        if (arrStr.length == 6) {  //add and edit operations
                            int num = Integer.parseInt(arrStr[0]);
                            newNum = arrStr[1];
                            newAuth = arrStr[2];
                            newName = arrStr[3];
                            newPubl = arrStr[4];
                            newYear = arrStr[5];
                            Object addingData[] = {newNum, newAuth, newAuth, newPubl, newYear};
                            if (num == 2) {
                                for (int i = 0; i < DTM.getRowCount(); i++) {
                                    if (DTM.getValueAt(i, 0).equals(arrStr[1])) { //ищем ряд, который заменить по ключу
                                        DTM.removeRow(i);
                                        DTM.addRow(addingData);
                                        DTM.fireTableDataChanged();
                                        String sql_edit = "UPDATE BOOKS SET  Author='" + newAuth + "'," +
                                                " Name='" + newName + "', Publisher='" + newPubl + "', PublishYear='" + newYear + "' " +
                                                "WHERE ISBN='" + newNum + "' ";
                                        int rs_update1 = sq.executeUpdate(sql_edit);
                                        if (rs_update1 <= 0) System.out.println("\nError occurred");
                                        out.write("The string was edited\n");
                                        out.flush();
                                    }
                                }
                            }
                                if (num == 1) {
                                    DTM.addRow(addingData);
                                    String sq_str_insert = "INSERT INTO BOOKS  VALUES ('" + newNum + "','" + newAuth + "','" +
                                            newName + "','" + newPubl + "','" + newYear + "')";
                                    int rs_update2 = sq.executeUpdate(sq_str_insert);  //возвращае кол-ство столбцов, на которое повлиял запрос
                                    out.write("The string was added\n");
                                    out.flush();
                                }
                            }
                        }
                        readmessage = null;
                    }

                    out.close();
                    in.close();
                    sock.close();
                    rs.close();
                    sq.close();
                    db.close();
                }

            }

    public static void main(String args[]) throws IOException {
        ServerSocket sock = null;
        try{
            sock = new ServerSocket(8000);
            while (true){
                new ClientThread(sock.accept()).start();
            }
        }
        catch (Exception e){}
    }
}
