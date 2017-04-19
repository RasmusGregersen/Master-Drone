package cirkelgpsdarude;

import java.util.HashMap;

/**
 * Created by LarsMyrup on 19/04/2017.
 */
public class CircleIntersect {

    private double alpha;
    private double beta;

    public static void main(String[] args)
    {
        new CircleIntersect();
    }

    public CircleIntersect() {
        double x0 = 5;
        double y0 = 12;
        double x1 = 8;
        double y1 = 10;
        double x2 = 14;
        double y2 = 12;
        double dronex = 3;
        double droney = 3;


        circleTest(x0, y0, x1, y1, x2, y2, dronex, droney);

        double[] a = new double[3];
        double[] b = new double[3];
        a = circleCalculator(x0, y0, x1, y1, alpha);
        b = circleCalculator(x1, y1, x2, y2, beta);

        System.out.println(a[0]);
        System.out.println(a[1]);
        System.out.println(a[2]);

        System.out.println(circleIntercept(a,b,x1,y1)[0]);
        System.out.println(circleIntercept(a,b,x1,y1)[1]);




    }

    public void circleTest(double x1, double y1, double x2, double y2, double x3, double y3, double dronex, double droney) {

        double a = Math.sqrt(Math.pow((Math.abs(-y2 + y1)) , 2) + Math.pow((Math.abs(-x2 + x1)) , 2));

        double b = Math.sqrt(Math.pow((Math.abs(-y3 + y2)) , 2) + Math.pow((Math.abs(-x3 + x2)) , 2));

        double c = Math.sqrt(Math.pow((Math.abs(-droney + y1)) , 2) + Math.pow((Math.abs(-dronex + x1)) , 2));

        double d = Math.sqrt(Math.pow((Math.abs(-droney + y2)) , 2) + Math.pow((Math.abs(-dronex + x2)) , 2));

        double e = Math.sqrt(Math.pow((Math.abs(-droney + y3)) , 2) + Math.pow((Math.abs(-dronex + x3)) , 2));

        this.alpha = Math.acos((c*c+d*d-a*a)/(2*c*d));

        this.beta = Math.acos((e*e+d*d-b*b)/(2*e*d));

        double cx1 = (0.5 * ((y2 - y1) / a ) * Math.sqrt(a*a/(Math.sin(alpha)*Math.sin(alpha)) - a * a) + 0.5 * x1 + 0.5 * x2);

        double cy1 = (0.5 * ((- x2 + x1) / a ) * Math.sqrt(a*a/(Math.sin(alpha)*Math.sin(alpha)) - a * a) + 0.5 * y1 + 0.5 * y2);

        double r1 = 0.5 * a / Math.sin(alpha);

        double cx2 = (0.5 * ((y3 - y2) / b ) * Math.sqrt(b*b/(Math.sin(beta)*Math.sin(beta)) - b * b) + 0.5 * x2 + 0.5 * x3);

        double cy2 = (0.5 * ((-x3 + x2) / b ) * Math.sqrt(b*b/(Math.sin(beta)*Math.sin(beta)) - b * b) + 0.5 * y2 + 0.5 * y3);

        double r2 = 0.5 * b / Math.sin(beta);

        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        System.out.println(d);
        System.out.println(e);
        System.out.println(alpha);
        System.out.println(beta);
        System.out.println(cx1);
        System.out.println(cy1);
        System.out.println(r1);
        System.out.println(cx2);
        System.out.println(cy2);
        System.out.println(r2);

    }

    public double[] circleCalculator(double x1, double y1, double x2, double y2, double alpha) {
        double a = Math.sqrt(Math.pow((Math.abs(-y2 + y1)) , 2) + Math.pow((Math.abs(-x2 + x1)) , 2));

        double[] array  = new double[3];

        array[0] = (0.5 * ((y2 - y1) / a ) * Math.sqrt(a*a/(Math.sin(alpha)*Math.sin(alpha)) - a * a) + 0.5 * x1 + 0.5 * x2);

        array[1] = (0.5 * ((- x2 + x1) / a ) * Math.sqrt(a*a/(Math.sin(alpha)*Math.sin(alpha)) - a * a) + 0.5 * y1 + 0.5 * y2);

        array[2] = 0.5 * a / Math.sin(alpha);

        return array;
    }

    public double[] circleIntercept(double[] c1, double[] c2, double x, double y) {

        double dx = c2[0] - c1[0];
        double dy = c2[1] - c1[1];

        double d = Math.sqrt((dy * dy) + (dx * dx));

        if (d > c1[2] + c2[2]) {
            return null;
        }

        if (d < Math.abs(c1[2] - c2[2])) {
            return null;
        }

        double a = (((c1[2] * c1[2]) - (c2[2] * c2[2]) + (d*d)) / (2*d));
        double x2 = c1[0] + (dx*a/d);
        double y2 = c1[1] + (dy*a/d);

        double h = Math.sqrt((c1[2]*c1[2]) - (a*a));

        double rx = (0-dy) * (h/d);
        double ry = dx * (h/d);

        double xi1 = x2 + rx;
        double xi2 = x2 - rx;
        double yi1 = y2 + ry;
        double yi2 = y2 - ry;

        if (xi1 == x && yi1 == y) {
            return new double[]{xi2, yi2};
        }
        else return new double[]{xi1, yi1};
    }

}
