package org.example;

import org.example.offline.*;
import org.example.offline.extractor.orderer.*;
import org.example.offline.extractor.tracer.*;
import org.example.offline.preprocessor.*;
import org.example.online.*;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class Main {
    public static void main(String[] args) {
        HashSet<TracePoint> set = new HashSet<>();
        Trace res = new Trace();
        String filePathInit = "./photos/img";
        for (int pic_num = 1; pic_num <= (args.length / 2); ++pic_num) {
            int dx = Integer.parseInt(args[2 * (pic_num - 1)]);
            int dy = Integer.parseInt(args[2 * (pic_num - 1) + 1]);

            String filePath = filePathInit + pic_num + ".png";
            try {
                File file = new File(filePath);
                BufferedImage image = ImageIO.read(file);

                CombinedPreprocessor preprocessor = new CombinedPreprocessor();
                ThinTracer thinTracer = new ThinTracer();
                GreedyGraphTracer graphTracer = new GreedyGraphTracer();
                CutOrderer orderer = new CutOrderer();

                TraceList strokeList = orderer.order(
                        graphTracer.trace(
                                thinTracer.trace(
                                        new Bitmap(preprocessor.apply(image, true))
                                )
                        )
                );

                for (int i = 0; i < strokeList.getTraces().size(); ++i) {
                    connectPath(strokeList.getTraces().get(i));
                }

                for (Trace stroke : strokeList.getTraces()) {
                    for (TracePoint point : stroke.getPoints()) {
                        int x = point.getX() + dx;
                        int y = point.getY() + dy;

                        final int R = 1;

                        for (int i = x - R; i <= x + R; ++i) {
                            for (int j = y - R; j <= y + R; ++j) {
                                if ((i - x) * (i - x) + (j - y) * (j - y) <= R * R) {
                                    TracePoint curr_point = new TracePoint(i, j);
                                    if (!set.contains(curr_point)) {
                                        set.add(curr_point);
                                        res.getPoints().add(curr_point);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (TracePoint point : res.getPoints()) {
            System.out.print(point.getX() + " " + point.getY() + " ");
        }
    }

    protected static void connectPath(Trace stroke) {
        for (int i = 0; i < stroke.getPoints().size() - 1; ++i) {
            TracePoint current = stroke.getPoints().get(i);
            TracePoint next = stroke.getPoints().get(i + 1);
            int dx = next.getX() - current.getX();
            int dy = next.getY() - current.getY();
            if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
                Trace missingPoints = calculateMissingPoints(current, next);
                stroke.getPoints().addAll(i + 1, missingPoints.getPoints());
                i += missingPoints.getPoints().size();
            }
        }
    }

    protected static Trace calculateMissingPoints(TracePoint start, TracePoint end) {
        Trace res = new Trace();
        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int x = start.getX();
        int y = start.getY();
        while (x != end.getX() || y != end.getY()) {
            if (x != end.getX()) {
                x += (dx > 0) ? 1 : -1;
            }
            if (y != end.getY()) {
                y += (dy > 0) ? 1 : -1;
            }
            res.getPoints().add(new TracePoint(x, y));
        }
        res.getPoints().remove(res.getPoints().size() - 1);
        return res;
    }
}