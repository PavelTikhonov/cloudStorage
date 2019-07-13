
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private String login;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("abs/server_storage/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("abs/server_storage/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }
            if (msg instanceof FileList){
                File[] files = new File("abs/server_storage").listFiles();
                FileList fl = new FileList();
                if (files != null) {
                    for (File f: files) {
                        fl.addFileDescriptionToList(new FileDescription(f.getName(), String.valueOf(f.length())));
                    }
                    ctx.writeAndFlush(fl);
                }
            }
            if (msg instanceof FileDelete){
                File file = new File("abs/server_storage/" + ((FileDelete) msg).getFilename());
                if (file.delete()) {
                    File[] files = new File("abs/server_storage").listFiles();
                    FileList fl = new FileList();
                    if (files != null) {
                        for (File f: files) {
                            fl.addFileDescriptionToList(new FileDescription(f.getName(), String.valueOf(f.length())));
                        }
                        ctx.writeAndFlush(fl);
                    }
                }
            }
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("abs/server_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                File[] files = new File("abs/server_storage").listFiles();
                FileList fl = new FileList();
                if (files != null) {
                    for (File f: files) {
                        fl.addFileDescriptionToList(new FileDescription(f.getName(), String.valueOf(f.length())));
                    }
                    ctx.writeAndFlush(fl);
                }
            }
            if(msg instanceof AuthRequest){
                String loginCheck = AuthService.checkLoginAndPass(((AuthRequest) msg).getLogin(), ((AuthRequest) msg).getPassword());
                AuthResult ay;
                if(login != null){
                    if(loginCheck.equals(login)){
                        ay = new AuthResult("no");
                    }
                }
                if(loginCheck != null){
                    this.login = loginCheck;
                    ay = new AuthResult("ok");
                } else {
                    ay = new AuthResult("no");
                }
                ctx.writeAndFlush(ay);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
