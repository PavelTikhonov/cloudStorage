
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private String login;
    private String cloudStorageWay = "abs/server_storage/";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get(cloudStorageWay + login + "/" + fr.getFilename()))) {

                    File file = new File(cloudStorageWay + login + "/" + fr.getFilename());
                    int bufSize = 1024 * 1024 * 10;
                    int partsCount = new Long(file.length() / bufSize).intValue();
                    if (file.length() % bufSize != 0) {
                        partsCount++;
                    }
                    FileMessage fmOut = new FileMessage(fr.getFilename(), -1, partsCount, new byte[bufSize]);
                    FileInputStream in = new FileInputStream(file);
                    for (int i = 0; i < partsCount; i++) {
                        int readedBytes = in.read(fmOut.data);
                        fmOut.partNumber = i + 1;
                        if (readedBytes < bufSize) {
                            fmOut.data = Arrays.copyOfRange(fmOut.data, 0, readedBytes);
                        }
                        ChannelFuture channelFuture = ctx.writeAndFlush(fmOut);
                        System.out.println("Отправлена часть #" + (i + 1));
                    }
                    in.close();
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

                boolean append = true;
                if (fm.partNumber == 1) {
                    append = false;
                }
                System.out.println(fm.partNumber + " / " + fm.partsCount);
                FileOutputStream fos = new FileOutputStream(cloudStorageWay + login + "/" + fm.filename, append);
                fos.write(fm.data);
                fos.close();
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
