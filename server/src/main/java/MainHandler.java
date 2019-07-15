
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private String login;
    private String cloudStorageWay = "abs/server_storage/";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get(cloudStorageWay + login + "/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get(cloudStorageWay + login + "/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }
            if (msg instanceof FileList){
                FileList fl = getFileList();
                ctx.writeAndFlush(fl);
            }
            if (msg instanceof FileDelete){
                File file = new File(cloudStorageWay + login + "/" + ((FileDelete) msg).getFilename());
                if (file.delete()) {
                    FileList fl = getFileList();
                    ctx.writeAndFlush(fl);
                }
            }
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get(cloudStorageWay + login + "/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                FileList fl = getFileList();
                ctx.writeAndFlush(fl);
            }
            if(msg instanceof AuthRequest){
                String loginCheck = AuthService.checkLoginAndPass(((AuthRequest) msg).getLogin(), ((AuthRequest) msg).getPassword());
                AuthResult ay;

                if(loginCheck != null){
                    this.login = loginCheck;
                    ay = new AuthResult("ok");
                } else {
                    ay = new AuthResult("no");
                }

                File file = new File(cloudStorageWay + login);
                if(!(file).exists())
                {
                    file.mkdir();
                }
                ctx.writeAndFlush(ay);
            }
            if(msg instanceof AuthClose){
                AuthService.closeConnectByLogin(((AuthClose) msg).getLogin());
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private FileList getFileList() {
        File[] files = new File(cloudStorageWay + login).listFiles();
        FileList fl = new FileList();
        if (files != null) {
            for (File f: files) {
                fl.addFileDescriptionToList(new FileDescription(f.getName(), String.valueOf(f.length())));
            }
        }
        return fl;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
