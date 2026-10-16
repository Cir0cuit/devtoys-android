package com.cir0cuit.devtoys.di

import com.cir0cuit.devtoys.tools.base.DefaultToolRegistry
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import com.cir0cuit.devtoys.tools.encoders.Base64Tool
import com.cir0cuit.devtoys.tools.generators.UUIDTool
import com.cir0cuit.devtoys.tools.crypto.KeyPairGeneratorTool
import com.cir0cuit.devtoys.tools.crypto.OtpTool
import com.cir0cuit.devtoys.tools.crypto.TextEncryptionTool
import com.cir0cuit.devtoys.tools.data.JsonDiffTool
import com.cir0cuit.devtoys.tools.data.XmlJsonTool
import com.cir0cuit.devtoys.tools.developer.ContrastCheckerTool
import com.cir0cuit.devtoys.tools.developer.IpConverterTool
import com.cir0cuit.devtoys.tools.developer.SemverTool
import com.cir0cuit.devtoys.tools.encoders.BaseNTool
import com.cir0cuit.devtoys.tools.encoders.GzipTool
import com.cir0cuit.devtoys.tools.generators.IdGeneratorTool
import com.cir0cuit.devtoys.tools.reference.BasicAuthTool
import com.cir0cuit.devtoys.tools.reference.DeviceInfoTool
import com.cir0cuit.devtoys.tools.reference.MathEvaluatorTool
import com.cir0cuit.devtoys.tools.reference.MimeTypeTool
import com.cir0cuit.devtoys.tools.reference.PortReferenceTool
import com.cir0cuit.devtoys.tools.reference.UnitConverterTool
import com.cir0cuit.devtoys.tools.text.FindReplaceTool
import com.cir0cuit.devtoys.tools.text.ListCompareTool
import com.cir0cuit.devtoys.tools.text.StringEscapeTool
import com.cir0cuit.devtoys.tools.text.TextStatsTool
import com.cir0cuit.devtoys.tools.text.UnicodeInspectorTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.cir0cuit.devtoys.tools.data.SqlFormatterTool
import com.cir0cuit.devtoys.tools.data.CsvViewerTool
import com.cir0cuit.devtoys.tools.developer.JsonToTypesTool
import com.cir0cuit.devtoys.tools.encoders.NumberBaseConverterTool
import com.cir0cuit.devtoys.tools.generators.LoremIpsumTool
import com.cir0cuit.devtoys.tools.encoders.UrlEncoderTool
import com.cir0cuit.devtoys.tools.encoders.HexEncoderTool
import com.cir0cuit.devtoys.tools.data.JsonFormatterTool
import com.cir0cuit.devtoys.tools.developer.RegexTesterTool
import com.cir0cuit.devtoys.tools.crypto.HashCalculatorTool
import com.cir0cuit.devtoys.tools.developer.JwtDecoderTool
import com.cir0cuit.devtoys.tools.developer.TimestampTool
import com.cir0cuit.devtoys.tools.text.CaseConverterTool
import com.cir0cuit.devtoys.tools.encoders.HtmlEntityTool
import com.cir0cuit.devtoys.tools.encoders.Rot13Tool
import com.cir0cuit.devtoys.tools.generators.QrGeneratorTool
import com.cir0cuit.devtoys.tools.text.TextDiffTool
import com.cir0cuit.devtoys.tools.text.LineToolsTool
import com.cir0cuit.devtoys.tools.data.YamlJsonTool
import com.cir0cuit.devtoys.tools.data.XmlFormatterTool
import com.cir0cuit.devtoys.tools.generators.PasswordGeneratorTool
import com.cir0cuit.devtoys.tools.crypto.HmacCalculatorTool
import com.cir0cuit.devtoys.tools.developer.CronTool
import com.cir0cuit.devtoys.tools.developer.ColorPickerTool
import com.cir0cuit.devtoys.tools.developer.MarkdownTool
import com.cir0cuit.devtoys.tools.reference.CheatsheetTool
import com.cir0cuit.devtoys.tools.reference.HttpStatusTool
import com.cir0cuit.devtoys.tools.developer.ChmodCalculatorTool
import com.cir0cuit.devtoys.tools.developer.CidrCalculatorTool
import com.cir0cuit.devtoys.tools.developer.MacAddressTool
import com.cir0cuit.devtoys.tools.crypto.X509DecoderTool

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideToolRegistry(): ToolRegistry {
        return DefaultToolRegistry(
            listOf(
                Base64Tool(),
                UUIDTool(),
                UrlEncoderTool(),
                HexEncoderTool(),
                JsonFormatterTool(),
                RegexTesterTool(),
                HashCalculatorTool(),
                JwtDecoderTool(),
                TimestampTool(),
                CaseConverterTool(),
                HtmlEntityTool(),
                Rot13Tool(),
                QrGeneratorTool(),
                TextDiffTool(),
                LineToolsTool(),
                YamlJsonTool(),
                XmlFormatterTool(),
                PasswordGeneratorTool(),
                HmacCalculatorTool(),
                CronTool(),
                ColorPickerTool(),
                MarkdownTool(),
                CheatsheetTool(),
                HttpStatusTool(),
                SqlFormatterTool(),
                CsvViewerTool(),
                JsonToTypesTool(),
                NumberBaseConverterTool(),
                LoremIpsumTool(),
                ChmodCalculatorTool(),
                CidrCalculatorTool(),
                MacAddressTool(),
                X509DecoderTool(),
                KeyPairGeneratorTool(),
                OtpTool(),
                TextEncryptionTool(),
                JsonDiffTool(),
                XmlJsonTool(),
                ContrastCheckerTool(),
                IpConverterTool(),
                SemverTool(),
                BaseNTool(),
                GzipTool(),
                IdGeneratorTool(),
                BasicAuthTool(),
                DeviceInfoTool(),
                MathEvaluatorTool(),
                MimeTypeTool(),
                PortReferenceTool(),
                UnitConverterTool(),
                FindReplaceTool(),
                ListCompareTool(),
                StringEscapeTool(),
                TextStatsTool(),
                UnicodeInspectorTool(),
                com.cir0cuit.devtoys.tools.developer.ColorPaletteTool(),
                com.cir0cuit.devtoys.tools.encoders.ImageBase64Tool()
            )
        )
    }
}
