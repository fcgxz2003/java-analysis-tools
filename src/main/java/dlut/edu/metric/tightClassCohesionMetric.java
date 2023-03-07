/*The relative number of method pairs of a class that access in common at least one attribute of the measured class.
TCC only counts direct attribute accesses, that is,
only those attributes that are accessed in the body of the method. */

package dlut.edu.metric;

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class tightClassCohesionMetric extends AbstractJavaRule {

    //度量项名
    private final String name = "类内函数间的耦合关系";
    //精度
    private final String precision = "4";

    public Object visit(ASTClassOrInterfaceDeclaration COID, Object data) {

        //度量名称
        String calculatorID = "tightClassCohesionMetric";
        //得到监控的数值
        float num_monitor = (float) JavaMetrics.get(JavaClassMetricKey.TCC, COID);

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
        String fileName = System.getProperty("user.dir") + "\\MetricTempFile.txt";
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
