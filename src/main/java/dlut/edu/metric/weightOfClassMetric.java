/* Number of “functional” public methods divided by the total number of public methods.
Our definition of “functional method” excludes constructors, getters, and setters.
This metric tries to quantify whether the measured class’ interface reveals more data than behaviour.
 Low values (less than 30%) indicate that the class reveals much more data than behaviour,
 which is a sign of poor encapsulation. */

package dlut.edu.metric;

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class weightOfClassMetric extends AbstractJavaRule {

    //度量项名
    private final String name = "类的功能函数的比例";
    //精度
    private final String precision = "4";

    public Object visit(ASTClassOrInterfaceDeclaration COID, Object data) {

        //度量名称
        String calculatorID = "weightOfClassMetric";
        //得到监控的数值
        float num_monitor = (float) JavaMetrics.get(JavaClassMetricKey.WOC, COID);

        //注意到如果监控的数值是NaN的话，说明函数对的数量为0，此时赋值为-1.
        if (Float.isNaN(num_monitor)) {
            num_monitor = -1;
        }
        //起始行数
        int begin_line = (int) COID.getBeginLine();
        //结束行数
        int end_line = (int) COID.getEndLine();
        //得到函数的位置
        String location = COID.getQualifiedName().toString();
        //设置输入到txt的内容
        String content = calculatorID + ";" + name + ";" + location + ";" + num_monitor + ";" + begin_line + ";"
                + end_line + ";" + precision + "\n";

        //写入的文件名
        String fileName = System.getProperty("user.dir")+"\\MetricTempFile.txt";
        //将内容写入
        File file = new File(fileName);
        try {
            FileWriter writer = new FileWriter(file, true); /*覆盖写入文件*/
            BufferedWriter out = new BufferedWriter(writer);
            out.append(content);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return super.visit(COID, data);
    }
}
