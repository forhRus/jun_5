public class app {
  public static void main(String[] args) {
    String[] msg = {
            "@nick",
            "@nick ",
            "@nick text",
            "@ nick text",
            " @nick text",
            "nick text"
    };

    for (int i = 0; i < msg.length; i++) {
      System.out.println(sendPrivate(msg[i]));
    }


  }

  private static String sendPrivate(String msg) {
    if(isPrivate(msg)){
      String name = getName(msg);
      if(name != null) {
        return name;
      }
      return "null";
    } else {
      return "не приват";
    }
  }

  private static boolean isPrivate(String name) {
    return name.trim().charAt(0) == '@';
  }

  private static String getName(String name) {
    int index = name.trim().indexOf(" ");
    if(index > 1){
      return name.trim().substring(1, index);
    }
    return null;
  }


}
