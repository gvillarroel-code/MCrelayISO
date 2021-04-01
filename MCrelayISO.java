// *** RELAY ISO PARA OMNICHANNEL/SIBLINK ***
// V1.stable
//
//********************************************

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.Format;
import java.text.SimpleDateFormat;


class MCrelayISO {
  public static int MaxConnections = 9000;
  public static BufferedInputStream BISpool[] = new BufferedInputStream[MaxConnections];
  public static BufferedOutputStream BOSpool[] = new BufferedOutputStream[MaxConnections];

  public static BufferedInputStream SLBISpool[] = new BufferedInputStream[MaxConnections];
  public static BufferedOutputStream SLBOSpool[] = new BufferedOutputStream[MaxConnections];
  public static int ConnPool[] = new int[MaxConnections];
  public static String ConnIpPool[] = new String[MaxConnections];
  public static int SLFreePool[] = new int[9];
  public static int ClienteID;
  public static int SLconnID;
  public static int SLport;
  public static int SLECOsendtimeout;
  public static int SLECOresptimeout;
  public static int CLIport;
  public static int MSGblocksize;
  public static String LOGecos;
  public static String LOGconn;
  public static String EsperaEstaciones;
  public static String DebugTramaCliente;
  public static String DebugTramaServidor;
  public static String LOGdata;
  public static FileWriter fw;
  public static BufferedWriter Bufferedfw;
  public static String KillConexionPorTimeOut;
  public static int KillConexionTimeOut;
  public static String SfbFechaHoy;


  public static void main(String args[]) throws Exception {

    File filefecha = new File("SFBFECHAHOT.txt");
    BufferedReader br = new BufferedReader(new FileReader(filefecha));
  
    SfbFechaHoy = br.readLine()
    System.out.println("\nFecha SFB\n");
    System.out.println("\n---------\n");
    System.out.println(SfbFechaHoy);
    System.out.println("\n---------\n");

    //  LEO ARCHIVO INI
    Properties p = new Properties();

    try {
      p.load(new FileInputStream("MCrelayISO.ini"));
    } catch (IOException e) {
      System.out.println(
        "\n\n ATENCION: Error al abrir el archivo de parametros MCrelayISO.ini \n\n "
      );
      System.exit(2);
    }

    System.out.println(" ");
    System.out.println("Variables de inicio (MCrelayISO.ini)");
    System.out.println("---------------------------------");
    System.out.println(
      "Puerto SibLink                    = " + p.getProperty("SLport")
    );
    System.out.println(
      "Puerto Cliente                    = " + p.getProperty("CLIport")
    );
    System.out.println(
      "TimeOut para enviar ECO a SibLink = " + p.getProperty("SLECOsendtimeout")
    );
    System.out.println(
      "TimeOut para Recibir respuesta ECO= " + p.getProperty("SLECOresptimeout")
    );
    System.out.println(
      "Loguea ECO                        = " + p.getProperty("LOGecos")
    );
    System.out.println(
      "Loguea Datos                      = " + p.getProperty("LOGdata")
    );
    System.out.println(
      "Loguea Conexiones                 = " + p.getProperty("LOGconn")
    );
    System.out.println(
      "Tamaño de bloque leido            = " + p.getProperty("MSGblocksize")
    );
    System.out.println(
      "Esperar 8 estaciones al inicio    = " + p.getProperty("EsperaEstaciones")
    );
    System.out.println(
      "Debug sobre trama CLiente         = " +
      p.getProperty("DebugTramaCliente")
    );
    System.out.println(
      "Debug sobre trama Servidor        = " +
      p.getProperty("DebugTramaServidor")
    );
    System.out.println(
      "Elimina conexion por Timeout      = " +
      p.getProperty("KillConexionPorTimeOut")
    );
    System.out.println(
      "Timeout para rliminar conexion   = " +
      p.getProperty("KillConexionTimeOut")
    );
    System.out.println("---------------------------------");
    System.out.println(" ");
    System.out.println(" ");

    MCrelayISO.SLport = Integer.parseInt(p.getProperty("SLport"));
    MCrelayISO.CLIport = Integer.parseInt(p.getProperty("CLIport"));
    MCrelayISO.SLECOsendtimeout =
      Integer.parseInt(p.getProperty("SLECOsendtimeout"));
    MCrelayISO.SLECOresptimeout =
      Integer.parseInt(p.getProperty("SLECOresptimeout"));
    MCrelayISO.MSGblocksize = Integer.parseInt(p.getProperty("MSGblocksize"));
    MCrelayISO.LOGecos = p.getProperty("LOGecos");
    MCrelayISO.LOGdata = p.getProperty("LOGdata");
    MCrelayISO.LOGconn = p.getProperty("LOGconn");
    MCrelayISO.EsperaEstaciones = p.getProperty("EsperaEstaciones");
    MCrelayISO.DebugTramaCliente = p.getProperty("DebugTramaCliente");
    MCrelayISO.DebugTramaServidor = p.getProperty("DebugTramaServidor");
    MCrelayISO.KillConexionPorTimeOut = p.getProperty("KillConexionPorTimeOut");
    MCrelayISO.KillConexionTimeOut =
      Integer.parseInt(p.getProperty("KillConexionTimeOut"));

    // INICIALIZA EL ARCHIVO DE LOG
    try {
      MCrelayISO.fw = new FileWriter("/var/log/MCrelayISO.log");
      MCrelayISO.Bufferedfw = new BufferedWriter(MCrelayISO.fw);

      MCrelayISO.Bufferedfw.write("Inicio del log...\n");
    } catch (Exception e) {
      System.out.println(e);
    }

    //INICIALIZA TABLA DE ASIGNACION DE POOL DE CONEXIONES
    for (
      int poolid = 0;
      poolid < MaxConnections;
      poolid++
    ) MCrelayISO.ConnPool[poolid] = -1;
    for (int poolid = 0; poolid < 9; poolid++) MCrelayISO.SLFreePool[poolid] =
      -1;

    // CREA LAS CONEXIONES ENTRANTES HACIA AMBOS EXTREMOS E INICIA LOS THREADS QUE LAS ATIENDEN
    //
    try {
      // CREO EL SOCKET HACIA SIBLINK
      ServerSocket ss = new ServerSocket(MCrelayISO.SLport);
      // CREA E INICIA EL THREAD QUE TRATA LAS CONEXIONES ENTRANTES DESDE SIBLINK
      Thread ts = new SiblinkHandler(ss);
      ts.start();
    } catch (Exception e) {
      System.out.println(
        "ATENCION: Error al abrir el puerto (" +
        MCrelayISO.SLport +
        "), Es posible que el Relay IP Siblink ya este iniciado ...\n\n"
      );
      System.exit(2);
    }

    try {
      // CREO EL SOCKET HACIA LOS CLIENTES
      ServerSocket sclientes = new ServerSocket(MCrelayISO.CLIport);
      // CREA EL THREAD QUE TRATA LAS CONEXIONES ENTRANTES HACIA SIBLINK, LE PASO EL SOCKET PRINCIPAL
      Thread tc = new ClienteHandler(sclientes);
      tc.start();
    } catch (Exception e) {
      System.out.println(
        "ATENCION: Error al abrirel puerto (" +
        MCrelayISO.CLIport +
        ")  Es posible que el Relay IP Siblink ya este iniciado ...\n\n"
      );
      System.exit(2);
    }
    //			MCrelayISO.fw.close();

  } // Fin de Main

  public static void MuestraConn() {
    // Muestro el estado del pool
    System.out.println("\n|-- SLNK - CLI --|");
    System.out.println("|--------+-------|");
    for (int p = 0; p < 8; p++) {
      System.out.println(
        "|-> " +
        String.format("%04d", MCrelayISO.SLFreePool[p]) +
        " + " +
        String.format("%04d", MCrelayISO.ConnPool[p]) +
        " -| ip: " +
        MCrelayISO.ConnIpPool[p]
      );
    }
    System.out.println(" ");
  }
} // Fin de Class Relay

class SiblinkHandler extends Thread {
  final ServerSocket ssa;

  // Constructor
  public SiblinkHandler(ServerSocket ssa) {
    this.ssa = ssa;
  }

  @Override
  public void run() {
    boolean Mainexit = true;
    MCrelayISO.SLconnID = 0;

    System.out.println("Esperando conexiones SibLink... :");
    //  INICIA EL LOOP PRINCIPAL QUE ACEPTA CONEXIONES
    while (Mainexit) {
      try { // Block 1
        Socket s = ssa.accept();

        // Busca una posicion libre (-1) En el pool de Siblink y asigna la conexion a ese pool
        for (int poolid = 0; poolid <= 8; poolid++) {
          if (MCrelayISO.SLFreePool[poolid] == -1) {
            System.out.println(
              "Conexion SibLink asignada a estacion: " +
              poolid +
              ") IP Remota: " +
              s.getRemoteSocketAddress().toString()
            );
            MCrelayISO.SLFreePool[poolid] = poolid;
            MCrelayISO.SLconnID = poolid;
            break;
          }
        }

        if (MCrelayISO.SLconnID < 8) {
          BufferedInputStream din = new BufferedInputStream(
            s.getInputStream(),
            2048
          );
          BufferedOutputStream dout = new BufferedOutputStream(
            s.getOutputStream(),
            2048
          );

          MCrelayISO.SLBISpool[MCrelayISO.SLconnID] = din;
          MCrelayISO.SLBOSpool[MCrelayISO.SLconnID] = dout;

          // create a new thread object
          Thread t = new SiblinkHandlerThread(
            s,
            din,
            dout,
            MCrelayISO.SLconnID
          );

          // Invoking the start() method
          t.start();
          MCrelayISO.MuestraConn();
        } else {
          System.out.println(
            "Conexion rechazada, maximo de 8 conexiones alcanzado..."
          );
          try {
            s.close();
          } catch (Exception e) {
            System.out.println("Exception: " + e);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        Mainexit = false;
      }
    } // Fin de While

    try {
      ssa.close();
    } catch (Exception e) {
      System.out.println("Exception: " + e);
    }
  }
}

// ClientHandler class
class SiblinkHandlerThread extends Thread {
  final BufferedInputStream din;
  final BufferedOutputStream dout;
  final Socket s;
  final int SLNroConn;

  public static Calendar x;
  public static String fecha;
  public static String tramafinalmmdd;
  public static String tramafinalhhmmss;
  public static Format f;


  static byte b[] = new byte[8192];
  String str = "";
//  str2 =
//    "ISO0040000400800822000000000000004000000000000001234561234567890301";

  String str2 =
    "ISO0140000100220B23A800128E0941A000000001600011AA50000000000000000mmddhhmmss000002hhmmssmmddmmddmmdd11((acq.ins))37<track3.............................>123456789012tes1tes1tes1tes1term.term.term.locationlocationlocationlocationlocation032pin.pin.pin.pin.023N99999999999ataadddata*012**term.dat**013*card issuer*016pinopinopinopino11recinstcode28052004664928++++++++++++++++28............................033termaddrtermaddrtermaddrtermaddr.001.001.043,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";

    
  String str4 =
    "ISO0140000130200B23A800128E0961E000000001600011AF31000000000000139mmddhhmmss015457hhmmssmmddmmddmmdd11           3799990386986532000=7412               000555      009900054       00000000000    00000000000000000000000000000000000000AR032FFFFFFFFFFFFFFFF023                       24764XJ8G7V95GWXD9EMPYR0Z          FAC0386272951964703860001003000046011133001004601113       0386230589027643860036205000040056918036004005691       4058960001082013   CASTRO, EMILCE EVANGELINA               NND0                                     012LINKLNK1+0000130386LNK11100P02554                       016                11614        28001004601113                28                            033000000000000000000000000000000000001 001 0430000000000000000000000000000000000000000000";

  String str5 =
     "ISO0140000130200B23A800128E0941E0000000014000010370000000000000000101514071499108614071410150   101511           373860027295196470                     991086      900100066       OMNI           00000000000000000000000000000000000000000323132333435363738120   0000001200  T                                                                                                        012OMNIOMN1+0000130386OMN11100P025OC                       016                11614        28                            001 ";

  int j, l;

  // Constructor
  public SiblinkHandlerThread(
    Socket s,
    BufferedInputStream din,
    BufferedOutputStream dout,
    int SLNroConn
  ) {
    this.s = s;
    this.din = din;
    this.dout = dout;
    this.SLNroConn = SLNroConn;
  }

  @Override
  public void run() {
    int i = 1;
    boolean exit = true;
    int msgi = 0;
    int msgj = 0;
    String str = "";
    int ISOsize = 0;

    // INICIA LOOP PRINCIPAL DEL HILO
    while (exit) {
      try {
        s.setSoTimeout(MCrelayISO.SLECOsendtimeout); // Si Excede timeout envio ECO a siblink

        // lee del stream de entrada del cliente, el primer byte
        // si esta en EOF, salgo
        if ((msgi = din.read()) == -1) {
          System.out.println(
            "======> Siblink cerro la conexion (" +
            SLNroConn +
            ") libero pool y cierro socket"
          );
          exit = false;
          break;
        }

        ISOsize = din.available() + 1;
        //				if (MCrelayISO.DebugTramaServidor.charAt(0) == (char) 'F' && exit) {System.out.println("Bytes disponibles en buffer in Siblink: "+ISOsize);}

        // lee del stream de entrada del cliente, el segundo byte
        // si esta en EOF, salgo
        if ((msgj = din.read()) == -1) {
          System.out.println(
            "======> Siblink cerro la conexion (" +
            SLNroConn +
            ") libero pool y cierro socket"
          );
          exit = false;
          break;
        }

        int msgsize = msgi * 256 + msgj;

        if (din.read(b, 0, ISOsize) == -1) {
          System.out.println(
            "======> Siblink cerro la conexion (" +
            SLNroConn +
            ") libero pool y cierro socket"
          );
          exit = false;
          break;
        }

        // Si estaa activo el Debug deja copia de la Trama en el archivo de log.
        if (MCrelayISO.DebugTramaServidor.charAt(0) == (char) 'F' && exit) { // INICIO LOGGINF
          str = "";
          for (int msgpos = 0; msgpos < msgsize; msgpos++) {
            str = str + (char) b[msgpos];
          }
          MCrelayISO.Bufferedfw.write(
            "\n\n ========== Respuesta desde Siblink (" +
            SLNroConn +
            ") IP:" +
            s.getRemoteSocketAddress().toString() +
            "  =========\n<< "
          );
          MCrelayISO.Bufferedfw.write(str);
          MCrelayISO.Bufferedfw.flush();
        } //FIN LOGGING

        if (MCrelayISO.ConnPool[SLNroConn] != -1 && exit) { //SOLO ESCRIBO SI EL OTRO EXTREMO ESTA VIVO
          MCrelayISO.BOSpool[MCrelayISO.ConnPool[SLNroConn]].write(msgi);
          MCrelayISO.BOSpool[MCrelayISO.ConnPool[SLNroConn]].write(msgj);
          MCrelayISO.BOSpool[MCrelayISO.ConnPool[SLNroConn]].write(
              b,
              0,
              msgsize
            );
          MCrelayISO.BOSpool[MCrelayISO.ConnPool[SLNroConn]].flush();
        }

        ISOsize = din.available();
        //				if (MCrelayISO.DebugTramaCliente.charAt(0) == (char) 'F' && exit) {System.out.println("Bytes disponibles en buffer in Siblink 2: "+ISOsize);}
      } catch (Exception e) {
        //				System.out.println("$$$> Excepcion:"+MCrelayISO.ConnPool[SLNroConn]+"--:"+e);
        if (EnvioEco(s, din, dout, SLNroConn) != 0) {
          exit = false;
          MCrelayISO.SLFreePool[SLNroConn] = -1;
          MCrelayISO.MuestraConn();
        }
      }
    } //FIN While
    // FIN DEL LOOP PRINCIPAL DEL HILO

    try {
      din.close();
      dout.close();
      s.close();
      MCrelayISO.SLFreePool[SLNroConn] = -1;
      MCrelayISO.MuestraConn();
    } catch (Exception e) {
      System.out.println("Exception: " + e);
    }
  } // Close run group

  // FUNCION QUE GENERA ECO PARA MANTENER VIVA LA CONEXION
  //
  public int EnvioEco(
    Socket s,
    BufferedInputStream din,
    BufferedOutputStream dout,
    int NroConnE
  ) {
    int i = 0;


//  MODIFICO VALORES DE LA TRAMA (str2) CON FECHA Y HORA   
//

    x = Calendar.getInstance();
    fecha = Integer.toString(x.get(x.MONTH) + 101).substring(1, 3) + Integer.toString(x.get(x.DATE) + 100).substring(1, 3);
    f = new SimpleDateFormat("HHmmss");
    tramafinalhhmmss = f.format(new Date());
    tramafinalmmdd = str2.replaceAll("mmdd", fecha);
    tramafinalhhmmss = tramafinalmmdd.replaceAll("hhmmss", tramafinalhhmmss);
    str2 = tramafinalhhmmss;

//


    //      Escribo ECO .....
    try {
      if (MCrelayISO.LOGecos.charAt(0) == (char) 'S') System.out.println(
        "Enviando ISO ECO a estacion SibLink (" + NroConnE + ")"
      );
      i = str2.length();
      for (j = 0; j < i; j++) b[j] = (byte) str2.charAt(j);
      dout.write(i / 256);
      dout.write(i % 256);
      dout.write(b, 0, i);
      dout.flush();
    } catch (Exception e) {
      System.out.println(
        "No Pude enviar ECO a estacion (" + NroConnE + ") Cerrando conexion"
      );
      try {
        din.close();
        dout.close();
        s.close();
        return 2;
      } catch (Exception ee) {
        System.out.println(
          "No pude cerrar el socket de la estacion: (" + NroConnE + " )"
        );
        return 2;
      }
    }

    //      Espero Respuesta del ECO .....
    try {
      s.setSoTimeout(MCrelayISO.SLECOresptimeout);
      i = din.read();
      j = din.read();
      din.read(b, 0, i * 256 + j);
      str = "";
      for (l = 0; l < (i * 256 + j); l++) str = str + (char) b[l];
      if (MCrelayISO.LOGecos.charAt(0) == (char) 'S') System.out.println(
        "Respuesta ISO ECO de estacion (" + NroConnE + ")Recibida.. "
      );
    } catch (Exception e) {
      System.out.println(
        "Respuesta ECO de estacion (" +
        NroConnE +
        ") no recibida.. Cerrando conexion  "
      );
      try {
        din.close();
        dout.close();
        s.close();
        return 2;
      } catch (Exception ee) {
        System.out.println(
          "No pude cerrar el socket de la estacion: (" + NroConnE + " )"
        );
        return 2;
      }
    }

    return 0;
  }
} // CLose class group

class ClienteHandler extends Thread {
  final ServerSocket ssb;

  // Constructor
  public ClienteHandler(ServerSocket ssb) {
    this.ssb = ssb;
  }

  @Override
  public void run() {
    boolean Mainexit = true;
    int NroConn = 0;
    MCrelayISO.ClienteID = 0;

    //  ESPERA QUE ESTEN DISPONIBLES LAS 8 ESTACIONES SIBLINK ANTES DE ACEPTAR CLIENTES
    //
    if (MCrelayISO.EsperaEstaciones.charAt(0) == 'S') {
      while (MCrelayISO.SLconnID < 7) {
        try {
          Thread.sleep(10000);
          System.out.println("Esperandoo Inicio de Siblink (10 Segs..)");
        } catch (InterruptedException e) {
          System.out.println("Esperando Inicio de Siblink (10 Segs..)");
        }
      }
    }

    System.out.println("======> Esperando conexion clientes... : ");
    //  INICIA EL LOOP PRINCIPAL QUE ACEPTA CONEXIONES DEL LADO CLIENTE
    while (Mainexit) {
      try { // Block 1
        Socket s = ssb.accept();
        System.out.println(
          "======> conexion de Cliente aceptada...: (" +
          MCrelayISO.ClienteID +
          ") IP Remota: " +
          s.getRemoteSocketAddress().toString()
        );

        BufferedInputStream Cdin = new BufferedInputStream(
          s.getInputStream(),
          2048
        );
        BufferedOutputStream Cdout = new BufferedOutputStream(
          s.getOutputStream(),
          2048
        );

        MCrelayISO.BISpool[MCrelayISO.ClienteID] = Cdin;
        MCrelayISO.BOSpool[MCrelayISO.ClienteID] = Cdout;

        // Asigna la nueva conexion cliente a una conexion del pool de Siblink
        // Si no hay mas conexiones disponibles en el pool, da mensaje de error y cierra conexion cliente
        int IDasignado = 0;
        for (int poolid = 0; poolid <= 8; poolid++) {
          if (
            MCrelayISO.ConnPool[poolid] == -1 &&
            MCrelayISO.SLFreePool[poolid] != -1
          ) {
            System.out.println(
              "======> Conexion cliente asignada a estacion: " + poolid
            );
            MCrelayISO.ConnPool[poolid] = MCrelayISO.ClienteID;
            MCrelayISO.ConnIpPool[poolid] =
              s.getRemoteSocketAddress().toString();
            IDasignado = poolid;
            break;
          }
        }

        if (IDasignado < 8) { // Si habian conexiones disponibles, creo nuevo thread
          Thread t = new ClienteHandlerThread(
            s,
            Cdin,
            Cdout,
            MCrelayISO.ClienteID
          );
          t.start();
        } else {
          System.out.println(
            "======> No hay conexiones disponibles en el pool, cerrando conexion " +
            IDasignado +
            " SLconnID " +
            MCrelayISO.SLconnID
          );
          try {
            s.close();
          } catch (Exception e) {
            System.out.println("Exception: " + e);
          }
        }

        //Incrementa el indice del array del bufferstream de clintes
        MCrelayISO.ClienteID++;
      } catch (Exception e) {
        System.out.println("Exception: " + e);
        Mainexit = false;
      }
    } // Fin de While

    try {
      ssb.close();
    } catch (Exception e) {
      System.out.println("Exception: " + e);
    }
  }
}

// ClientHandler class
class ClienteHandlerThread extends Thread {
  final BufferedInputStream Cdin;
  final BufferedOutputStream Cdout;
  final Socket s;
  final int ClienteID;
  static byte b[] = new byte[2048];
  String str = "";

  // Constructor
  public ClienteHandlerThread(
    Socket s,
    BufferedInputStream Cdin,
    BufferedOutputStream Cdout,
    int ClienteID
  ) {
    this.s = s;
    this.Cdin = Cdin;
    this.Cdout = Cdout;
    this.ClienteID = ClienteID;
  }

  @Override
  public void run() {
    int i = 1;
    boolean exit = true;
    int a = 0;
    int msgi;
    int msgj;
    int ISOsize = 0;

    MCrelayISO.MuestraConn();

    // INICIA LOOP PRINCIPAL DEL HILO
    // Busca que conexion le corresponde del pool de siblink y se comunica con esa conexion
    for (int index = 0; index < 8; index++) {
      if (MCrelayISO.ConnPool[index] == ClienteID) {
        i = index;
        break;
      }
    }

    if (MCrelayISO.KillConexionPorTimeOut.charAt(0) == (char) 'S') {
      System.out.println(
        "La conexion se cerrara a los (" +
        MCrelayISO.KillConexionTimeOut +
        ") milisegundos de inactividad"
      );
    }

    while (exit) { //loop principal de transferencia de datos desde el cliente
      try {
        if (MCrelayISO.KillConexionPorTimeOut.charAt(0) == (char) 'S') {
          s.setSoTimeout(MCrelayISO.KillConexionTimeOut); // Si Excede timeout Mato la conexion cliente
        }

        // lee del stream de entrada del cliente, el primer byte
        // si esta en EOF, salgo
        if ((msgi = Cdin.read()) == -1) {
          System.out.println(
            "======> El extremo cerro la conexion.. libero pool y cierro socket"
          );
          exit = false;
          break;
        }

        ISOsize = Cdin.available() + 1;
        // if (MCrelayISO.DebugTramaCliente.charAt(0) == (char) 'F' && exit) {System.out.println("Bytes disponibles en buffer In Cliente inicio: "+ISOsize);}

        // lee del stream de entrada del cliente, el segundo byte
        // si esta en EOF, salgo
        if ((msgj = Cdin.read()) == -1) {
          System.out.println(
            "======> El extremo cerro la conexion.. libero pool y cierro socket"
          );
          exit = false;
          break;
        }

        int msgsize = msgi * 256 + msgj;

        // lee del stream de entrada del cliente, el Mensaje ISO
        // si esta en EOF, salgo
        if (Cdin.read(b, 0, ISOsize) == -1) {
          System.out.println(
            "======> El extremo cerro la conexion.. libero pool y cierro socket"
          );
          exit = false;
        }

        // Si estaa activo el Debug deja copia de la Trama en el archivo de log.
        if (MCrelayISO.DebugTramaCliente.charAt(0) == (char) 'F' && exit) {
          str = "";
          for (int msgpos = 0; msgpos < msgsize; msgpos++) {
            str = str + (char) b[msgpos];
          }
          MCrelayISO.Bufferedfw.write(
            "\n\n ========== Mensaje desde cliente (" +
            ClienteID +
            ") IP remota: " +
            s.getRemoteSocketAddress().toString() +
            "  ==bytes (" +
            msgsize +
            ")==\n>>"
          );
          MCrelayISO.Bufferedfw.write(str);
          MCrelayISO.Bufferedfw.flush();
        } //FIN LOGGING

        if (MCrelayISO.SLFreePool[i] != -1 && exit) {
          MCrelayISO.SLBOSpool[i].write(msgi);
          MCrelayISO.SLBOSpool[i].write(msgj);
          MCrelayISO.SLBOSpool[i].write(b, 0, msgsize);
          MCrelayISO.SLBOSpool[i].flush();
        }
        ISOsize = Cdin.available();
        //                               if (MCrelayISO.DebugTramaCliente.charAt(0) == (char) 'F' && exit) {System.out.println("Bytes disponibles en buffer In Cliente Fin: "+ISOsize);}
      } catch (SocketTimeoutException e) {
        System.out.println("Conexion cliente cerrada por inactividad..");
        exit = false;
      } catch (IOException e) {
        System.out.println(
          "======> Conexion cerrada por el cliente (" +
          ClienteID +
          ") IP Remota: " +
          s.getRemoteSocketAddress().toString()
        );
        exit = false;
      } catch (IndexOutOfBoundsException e) {
        System.out.println(
          "Mensaje ISO Mal formado o el cliente cerro la conexion.."
        );
      }
    } //FIN While

    // FIN DEL LOOP PRINCIPAL DEL HILO

    for (int poolid = 0; poolid < 9; poolid++) {
      if (MCrelayISO.ConnPool[poolid] == ClienteID) {
        System.out.println(
          "======> Conexion cliente (" +
          ClienteID +
          ") liberada del pool, estacion: " +
          poolid
        );
        MCrelayISO.ConnPool[poolid] = -1;
        MCrelayISO.ConnIpPool[poolid] = null;
        MCrelayISO.MuestraConn();
      }
    }

    try {
      Cdin.close();
      Cdout.close();
      s.close();
    } catch (Exception e) {
      System.out.println("Exception: " + e);
    }
  }
}
