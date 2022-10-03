import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class RetrofitterProcessor(
    val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private val funSet = mutableSetOf<FunSpec>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("Retrofitter")
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .toList()

        symbols.forEach { it.accept(RetrofitterVisitor(), Unit) }
        symbols.firstOrNull()?.accept(RetrofitterFileVisitor(), Unit)

        return ret
    }

    inner class RetrofitterFileVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val packageName = classDeclaration.containingFile!!.packageName.asString()
            val module = ClassName("dagger", "Module")
            val mainName = "RetrofitComponentsModule"

            val mainClass = TypeSpec
                .objectBuilder(mainName)
                .addAnnotation(module)
                .addFunctions(funSet)
                .build()
            FileSpec
                .builder(packageName, mainName)
                .addType(mainClass)
                .build()
                .writeTo(codeGenerator, true)
        }
    }

    inner class RetrofitterVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val newName = classDeclaration.simpleName.asString()
                .substringBefore(delimiter = "Interface", missingDelimiterValue = "")
            when (newName.isEmpty()) {
                true -> logger.error("Interface not found")
                false -> {
                    val retrofit = ClassName("retrofit2", "Retrofit")
                    val provides = ClassName("dagger", "Provides")
                    val singleton = ClassName("javax.inject", "Singleton")

                    val method = FunSpec
                        .builder("provides${newName}Client")
                        .addParameter("retrofit", retrofit)
                        .addAnnotation(provides)
                        .addAnnotation(singleton)
                        .returns(classDeclaration.toClassName())
                        .addStatement("return retrofit.create(${classDeclaration.simpleName.asString()}::class.java)")
                        .build()

                    funSet.add(method)
                }
            }
        }
    }
}

class RetrofitterProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return RetrofitterProcessor(environment.codeGenerator, environment.logger)
    }
}
