package com.cir0cuit.devtoys.di

import com.cir0cuit.devtoys.tools.base.DefaultToolRegistry
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import com.cir0cuit.devtoys.tools.encoders.Base64Tool
import com.cir0cuit.devtoys.tools.generators.UUIDTool
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
                com.cir0cuit.devtoys.tools.developer.ColorPaletteTool(),
                com.cir0cuit.devtoys.tools.encoders.ImageBase64Tool()
            )
        )
    }
}
